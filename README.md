# raft-kv
分布式kv存储
- 1.基于netty实现RPC注册中心、服务中心、客户端，
实现限流、负载均衡、心跳、多方式序列化，简单实现降级
 TODO：多注册中心，如：zookeeper；服务降级和服务限流；集群容错：failover，failfast。。。
- 2.实现raft算法，保证一致性
  TODO: 快照压缩
  参考：https://github.com/stateIs0/lu-raft-kv
- 3.模拟HBase实现简易的kv存储引擎，具有基本的put、get、scan、flush、compact基本功能，
  利用日志写的方式保证数据不丢失，构建布隆过滤器提高检索效率。
  TODO：缓存部分：如LRU
