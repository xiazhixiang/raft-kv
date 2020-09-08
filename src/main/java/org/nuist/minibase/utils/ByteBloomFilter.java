package org.nuist.minibase.utils;



import org.nuist.minibase.hash.Hash;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Implements a <i>Bloom filter</i>, as defined by Bloom in 1970.
 * <p>
 * The Bloom filter is a data structure that was introduced in 1970 and that has
 * been adopted by the networking research community in the past decade thanks
 * to the bandwidth efficiencies that it offers for the transmission of set
 * membership information between networked hosts. A sender encodes the
 * information into a bit vector, the Bloom filter, that is more compact than a
 * conventional representation. Computation and space costs for construction are
 * linear in the number of elements. The receiver uses the filter to test
 * whether various elements are members of the set. Though the filter will
 * occasionally return a false positive, it will never return a false negative.
 * When creating the filter, the sender can choose its desired point in a
 * trade-off between the false positive rate and the size.
 *
 * <p>
 * Originally inspired by <a href="http://www.one-lab.org">European Commission
 * One-Lab Project 034819</a>.
 * <p>
 * Bloom filters are very sensitive to the number of elements inserted into
 * them. For HBase, the number of entries depends on the size of the data stored
 * in the column. Currently the default region size is 256MB, so entry count ~=
 * 256MB / (average value size for column). Despite this rule of thumb, there is
 * no efficient way to calculate the entry count after compactions. Therefore,
 * it is often easier to use a dynamic bloom filter that will add extra space
 * instead of allowing the error rate to grow.
 * <p>
 * ( http://www.eecs.harvard.edu/~michaelm/NEWWORK/postscripts/BloomFilterSurvey
 * .pdf )
 * <p>
 * m denotes the number of bits in the Bloom filter (bitSize) n denotes the
 * number of elements inserted into the Bloom filter (maxKeys) k represents the
 * number of hash functions used (nbHash) e represents the desired false
 * positive rate for the bloom (err)
 * <p>
 * If we fix the error rate (e) and know the number of entries, then the optimal
 * bloom size m = -(n * ln(err) / (ln(2)^2) ~= n * ln(err) / ln(0.6185)
 * <p>
 * The probability of false positives is minimized when k = m/n ln(2).
 *
 * @see <a
 * href="http://portal.acm.org/citation.cfm?id=362692&dl=ACM&coll=portal">
 * Space/Time Trade-Offs in Hash Coding with Allowable Errors</a>
 */

public class ByteBloomFilter {

    /**
     * Current file format version
     */
    public static final int VERSION = 1;
    /**
     * Record separator for the Bloom filter statistics human-readable string
     */
    public static final String STATS_RECORD_SEP = "; ";
    /**
     * Used in computing the optimal Bloom filter size. This approximately equals
     * 0.480453.
     */
    public static final double LOG2_SQUARED = Math.log(2) * Math.log(2);
    /**
     * Bit-value lookup array to prevent doing the same work over and over
     */
    private static final byte[] bitvals = {
            (byte) 0x01,
            (byte) 0x02,
            (byte) 0x04,
            (byte) 0x08,
            (byte) 0x10,
            (byte) 0x20,
            (byte) 0x40,
            (byte) 0x80
    };
    /**
     * A random number generator to use for "fake lookups" when testing to
     * estimate the ideal false positive rate.
     */
    private static Random randomGeneratorForTest;
    /**
     * Hash type
     */
    protected final int hashType;
    /**
     * Hash Function
     */
    protected final Hash hash;
    /**
     * Bytes (B) in the array. This actually has to fit into an int.
     */
    protected long byteSize;
    /**
     * Number of hash functions
     */
    protected int hashCount;
    /**
     * Keys currently in the bloom
     */
    protected int keyCount;
    /**
     * Max Keys expected for the bloom
     */
    protected int maxKeys;
    /**
     * Bloom bits
     */
    protected ByteBuffer bloom;

    /**
     * Private constructor used by other constructors.
     */
    private ByteBloomFilter(int hashType) {
        this.hashType = hashType;
        this.hash = Hash.getInstance(hashType);
    }

    public ByteBloomFilter(int hashType, ByteBuffer bloom) {
        this(hashType);
        this.bloom = bloom;
    }

    /**
     * Determines & initializes bloom filter meta data from user config. Call
     * {@link #allocBloom()} to allocate bloom filter data.
     *
     * @param maxKeys    bloom过滤器可以存放的key的最大值。
     * @param errorRate  布隆过滤器判断错误的概率。
     * @param hashType   使用的hash函数
     * @param foldFactor When finished adding entries, you may be able to 'fold'
     *                   this bloom to save space. Tradeoff potentially excess bytes in
     *                   bloom for ability to fold if keyCount is exponentially greater
     *                   than maxKeys.
     * @throws IllegalArgumentException
     */
    public ByteBloomFilter(int maxKeys, double errorRate, int hashType,
                           int foldFactor) throws IllegalArgumentException {
        //设置hash函数
        this(hashType);
        //计算在这个错误概率下需要申请的空间大小
        long bitSize = computeBitSize(maxKeys, errorRate);
        //计算需要的hash函数数量
        hashCount = optimalFunctionCount(maxKeys, bitSize);
        this.maxKeys = maxKeys;

        // increase byteSize so folding is possible
        byteSize = computeFoldableByteSize(bitSize, foldFactor);

        sanityCheck();
    }

    /**
     * @param maxKeys
     * @param errorRate
     * @return the number of bits for a Bloom filter than can hold the given
     * number of keys and provide the given error rate, assuming that the
     * optimal number of hash functions is used and it does not have to
     * be an integer.
     */
    public static long computeBitSize(long maxKeys, double errorRate) {
        return (long) Math.ceil(maxKeys * (-Math.log(errorRate) / LOG2_SQUARED));
    }

    /**
     * The maximum number of keys we can put into a Bloom filter of a certain
     * size to maintain the given error rate, assuming the number of hash
     * functions is chosen optimally and does not even have to be an integer
     * (hence the "ideal" in the function name).
     *
     * @param bitSize
     * @param errorRate
     * @return maximum number of keys that can be inserted into the Bloom filter
     * @see #computeMaxKeys(long, double, int) for a more precise estimate
     */
    public static long idealMaxKeys(long bitSize, double errorRate) {
        // The reason we need to use floor here is that otherwise we might put
        // more keys in a Bloom filter than is allowed by the target error rate.
        return (long) (bitSize * (LOG2_SQUARED / -Math.log(errorRate)));
    }

    /**
     * The maximum number of keys we can put into a Bloom filter of a certain
     * size to get the given error rate, with the given number of hash functions.
     *
     * @param bitSize
     * @param errorRate
     * @param hashCount
     * @return the maximum number of keys that can be inserted in a Bloom filter
     * to maintain the target error rate, if the number of hash functions
     * is provided.
     */
    public static long computeMaxKeys(long bitSize, double errorRate,
                                      int hashCount) {
        return (long) (-bitSize * 1.0 / hashCount *
                Math.log(1 - Math.exp(Math.log(errorRate) / hashCount)));
    }

    /**
     * Computes the actual error rate for the given number of elements, number
     * of bits, and number of hash functions. Taken directly from the
     * <a href=
     * "http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives"
     * > Wikipedia Bloom filter article</a>.
     *
     * @param maxKeys
     * @param bitSize
     * @param functionCount
     * @return the actual error rate
     */
    public static double actualErrorRate(long maxKeys, long bitSize,
                                         int functionCount) {
        return Math.exp(Math.log(1 - Math.exp(-functionCount * maxKeys * 1.0
                / bitSize)) * functionCount);
    }

    /**
     * Increases the given byte size of a Bloom filter until it can be folded by
     * the given factor.
     *
     * @param bitSize
     * @param foldFactor
     * @return Foldable byte size
     */
    public static int computeFoldableByteSize(long bitSize, int foldFactor) {
        //获得存储bitSize个数据需要多少byte。
        long byteSizeLong = (bitSize + 7) / 8;
        //foldfactory 为2的幂减1
        int mask = (1 << foldFactor) - 1;
        //
        if ((mask & byteSizeLong) != 0) {
            byteSizeLong >>= foldFactor;
            ++byteSizeLong;
            byteSizeLong <<= foldFactor;
        }
        if (byteSizeLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("byteSize=" + byteSizeLong + " too "
                    + "large for bitSize=" + bitSize + ", foldFactor=" + foldFactor);
        }
        return (int) byteSizeLong;
    }

    private static int optimalFunctionCount(int maxKeys, long bitSize) {
        long i = bitSize / maxKeys;
        double result = Math.ceil(Math.log(2) * i);
        if (result > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("result too large for integer value.");
        }
        return (int) result;
    }

    /**
     * Creates a Bloom filter of the given size.
     *
     * @param byteSizeHint the desired number of bytes for the Bloom filter bit
     *                     array. Will be increased so that folding is possible.
     * @param errorRate    target false positive rate of the Bloom filter
     * @param hashType     Bloom filter hash function type
     * @param foldFactor
     * @return the new Bloom filter of the desired size
     */
    public static ByteBloomFilter createBySize(int byteSizeHint,
                                               double errorRate, int hashType, int foldFactor) {
        ByteBloomFilter bbf = new ByteBloomFilter(hashType);

        bbf.byteSize = computeFoldableByteSize(byteSizeHint * 8L, foldFactor);
        long bitSize = bbf.byteSize * 8;
        bbf.maxKeys = (int) idealMaxKeys(bitSize, errorRate);
        bbf.hashCount = optimalFunctionCount(bbf.maxKeys, bitSize);

        // Adjust max keys to bring error rate closer to what was requested,
        // because byteSize was adjusted to allow for folding, and hashCount was
        // rounded.
        bbf.maxKeys = (int) computeMaxKeys(bitSize, errorRate, bbf.hashCount);

        return bbf;
    }

    public static boolean contains(byte[] buf, int offset, int length,
                                   ByteBuffer bloomBuf, int bloomOffset, int bloomSize, Hash hash,
                                   int hashCount) {

        int hash1 = hash.hash(buf, offset, length, 0);
        int hash2 = hash.hash(buf, offset, length, hash1);
        int bloomBitSize = bloomSize << 3;

        if (randomGeneratorForTest == null) {
            // Production mode.
            int compositeHash = hash1;
            for (int i = 0; i < hashCount; i++) {
                int hashLoc = Math.abs(compositeHash % bloomBitSize);
                compositeHash += hash2;
                if (!get(hashLoc, bloomBuf, bloomOffset)) {
                    return false;
                }
            }
        } else {
            // Test mode with "fake lookups" to estimate "ideal false positive rate".
            for (int i = 0; i < hashCount; i++) {
                int hashLoc = randomGeneratorForTest.nextInt(bloomBitSize);
                if (!get(hashLoc, bloomBuf, bloomOffset)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if bit at specified index is 1.
     *
     * @param pos index of bit
     * @return true if bit at specified index is 1, false if 0.
     */
    static boolean get(int pos, ByteBuffer bloomBuf, int bloomOffset) {
        int bytePos = pos >> 3; //pos / 8
        int bitPos = pos & 0x7; //pos % 8
        // TODO access this via Util API which can do Unsafe access if possible(?)
        byte curByte = bloomBuf.get(bloomOffset + bytePos);
        curByte &= bitvals[bitPos];
        return (curByte != 0);
    }

    public static void setFakeLookupMode(boolean enabled) {
        if (enabled) {
            randomGeneratorForTest = new Random(283742987L);
        } else {
            randomGeneratorForTest = null;
        }
    }

    /**
     * Computes the error rate for this Bloom filter, taking into account the
     * actual number of hash functions and keys inserted. The return value of
     * this function changes as a Bloom filter is being populated. Used for
     * reporting the actual error rate of compound Bloom filters when writing
     * them out.
     *
     * @return error rate for this particular Bloom filter
     */
    public double actualErrorRate() {
        return actualErrorRate(keyCount, byteSize * 8, hashCount);
    }

    /**
     * Creates another similar Bloom filter. Does not copy the actual bits, and
     * sets the new filter's key count to zero.
     *
     * @return a Bloom filter with the same configuration as this
     */
    public ByteBloomFilter createAnother() {
        ByteBloomFilter bbf = new ByteBloomFilter(hashType);
        bbf.byteSize = byteSize;
        bbf.hashCount = hashCount;
        bbf.maxKeys = maxKeys;
        return bbf;
    }

    public void allocBloom() {
        if (this.bloom != null) {
            throw new IllegalArgumentException("can only create bloom once.");
        }
        this.bloom = ByteBuffer.allocate((int) this.byteSize);
        assert this.bloom.hasArray();
    }

    void sanityCheck() throws IllegalArgumentException {
        if (0 >= this.byteSize || this.byteSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid byteSize: " + this.byteSize);
        }

        if (this.hashCount <= 0) {
            throw new IllegalArgumentException("Hash function count must be > 0");
        }

        if (this.hash == null) {
            throw new IllegalArgumentException("hashType must be known");
        }

        if (this.keyCount < 0) {
            throw new IllegalArgumentException("must have positive keyCount");
        }
    }

    void bloomCheck(ByteBuffer bloom) throws IllegalArgumentException {
        if (this.byteSize != bloom.limit()) {
            throw new IllegalArgumentException(
                    "Configured bloom length should match actual length");
        }
    }

    public void add(byte[] buf) {
        add(buf, 0, buf.length);
    }

    public void add(byte[] buf, int offset, int len) {
        /*
         * For faster hashing, use combinatorial generation
         * http://www.eecs.harvard.edu/~kirsch/pubs/bbbf/esa06.pdf
         */
        int hash1 = this.hash.hash(buf, offset, len, 0);
        int hash2 = this.hash.hash(buf, offset, len, hash1);

        for (int i = 0; i < this.hashCount; i++) {
            long hashLoc = Math.abs((hash1 + i * hash2) % (this.byteSize * 8));
            set(hashLoc);
        }

        ++this.keyCount;
    }

    /**
     * Should only be used in tests
     */
    public boolean contains(byte[] buf) {
        return contains(buf, 0, buf.length, this.bloom);
    }

    /**
     * Should only be used in tests
     */
    boolean contains(byte[] buf, int offset, int length) {
        return contains(buf, offset, length, bloom);
    }

    //---------------------------------------------------------------------------
    /** Private helpers */

    /**
     * Should only be used in tests
     */
    boolean contains(byte[] buf, ByteBuffer bloom) {
        return contains(buf, 0, buf.length, bloom);
    }

    public boolean contains(byte[] buf, int offset, int length,
                            ByteBuffer theBloom) {
        if (theBloom == null) {
            // In a version 1 HFile Bloom filter data is stored in a separate meta
            // block which is loaded on demand, but in version 2 it is pre-loaded.
            // We want to use the same API in both cases.
            theBloom = bloom;
        }

        if (theBloom.limit() != byteSize) {
            throw new IllegalArgumentException("Bloom does not match expected size:"
                    + " theBloom.limit()=" + theBloom.limit() + ", byteSize=" + byteSize);
        }

        return contains(buf, offset, length, theBloom, 0, (int) byteSize, hash, hashCount);
    }

    /**
     * Set the bit at the specified index to 1.
     *
     * @param pos index of bit
     */
    void set(long pos) {
        int bytePos = (int) (pos / 8);
        int bitPos = (int) (pos % 8);
        byte curByte = bloom.get(bytePos);
        curByte |= bitvals[bitPos];
        bloom.put(bytePos, curByte);
    }

    public long getKeyCount() {
        return keyCount;
    }

    public long getMaxKeys() {
        return maxKeys;
    }

    public long getByteSize() {
        return byteSize;
    }

    public int getHashType() {
        return hashType;
    }


    //---------------------------------------------------------------------------

    public void compactBloom() {
        // see if the actual size is exponentially smaller than expected.
        if (this.keyCount > 0 && this.bloom.hasArray()) {
            int pieces = 1;
            //byte的数量
            int newByteSize = (int) this.byteSize;
            //放进去的key的数量
            int newMaxKeys = this.maxKeys;

            // while exponentially smaller & folding is lossless
            while ((newByteSize & 1) == 0 && newMaxKeys > (this.keyCount << 1)) {
                pieces <<= 1;
                newByteSize >>= 1;
                newMaxKeys >>= 1;
            }

            // if we should fold these into pieces
            if (pieces > 1) {
                byte[] array = this.bloom.array();
                int start = this.bloom.arrayOffset();
                int end = start + newByteSize;
                int off = end;
                for (int p = 1; p < pieces; ++p) {
                    for (int pos = start; pos < end; ++pos) {
                        array[pos] |= array[off++];
                    }
                }
                // folding done, only use a subset of this array
                this.bloom.rewind();
                this.bloom.limit(newByteSize);
                this.bloom = this.bloom.slice();
                this.byteSize = newByteSize;
                this.maxKeys = newMaxKeys;
            }
        }
    }

    /**
     * Writes just the bloom filter to the output array
     *
     * @param out OutputStream to place bloom
     * @throws IOException Error writing bloom array
     */
    public void writeBloom(final OutputStream out) throws IOException {
        if (!this.bloom.hasArray()) {
            throw new IOException("Only writes ByteBuffer with underlying array.");
        }
        out.write(bloom.array(), bloom.arrayOffset(), bloom.limit());
    }

    public int getHashCount() {
        return hashCount;
    }

    public boolean supportsAutoLoading() {
        return bloom != null;
    }

    /**
     * {@inheritDoc}
     * Just concatenate row and column by default. May return the original row
     * buffer if the column qualifier is empty.
     */
    public byte[] createBloomKey(byte[] rowBuf, int rowOffset, int rowLen,
                                 byte[] qualBuf, int qualOffset, int qualLen) {
        // Optimize the frequent case when only the row is provided.
        if (qualLen <= 0 && rowOffset == 0 && rowLen == rowBuf.length)
            return rowBuf;

        byte[] result = new byte[rowLen + qualLen];
        System.arraycopy(rowBuf, rowOffset, result, 0, rowLen);
        if (qualLen > 0)
            System.arraycopy(qualBuf, qualOffset, result, rowLen, qualLen);
        return result;
    }
}
