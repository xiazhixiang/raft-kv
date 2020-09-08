package org.nuist.raft.rpc.myRpc;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;
import org.nuist.rpc.common.transport.body.CommonCustomBody;
import org.nuist.raft.client.ClientKVReq;
import org.nuist.raft.entity.AentryParam;
import org.nuist.raft.entity.RvoteParam;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Request<T> implements CommonCustomBody, Serializable {

    /** 请求投票 */
    public static final int R_VOTE = 0;
    /** 附加日志 */
    public static final int A_ENTRIES = 1;
    /** 客户端 */
    public static final int CLIENT_REQ = 2;
    /** 配置变更. add*/
    public static final int CHANGE_CONFIG_ADD = 3;
    /** 配置变更. remove*/
    public static final int CHANGE_CONFIG_REMOVE = 4;
    /** 请求类型 */
    private int cmd = -1;

    /** param
     * @see AentryParam
     * @see RvoteParam
     * @see ClientKVReq
     * */
    private T obj;

    String url;

    public Request() {
    }

    public Request(T obj) {
        this.obj = obj;
    }

    public Request(int cmd, T obj, String url) {
        this.cmd = cmd;
        this.obj = obj;
        this.url = url;
    }

    private Request(Request.Builder builder) {
        setCmd(builder.cmd);
        setObj((T) builder.obj);
        setUrl(builder.url);
    }

    public static Request.Builder newBuilder() {
        return new Request.Builder<>();
    }


    public final static class Builder<T> {

        private int cmd;
        private Object obj;
        private String url;

        private Builder() {
        }

        public Request.Builder cmd(int val) {
            cmd = val;
            return this;
        }

        public Request.Builder obj(Object val) {
            obj = val;
            return this;
        }

        public Request.Builder url(String val) {
            url = val;
            return this;
        }

        public Request<T> build() {
            return new Request<T>(this);
        }
    }

    @Override
    public void checkFields() throws RemotingCommmonCustomException {

    }
}
