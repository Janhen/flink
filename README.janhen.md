### 基本操作
SingleOutputStreamOperator  
KeyedStream  
PatternStream   
    - PatternSelectFunction
    - PatternTimeoutFunction
IterativeStream   
AllWindowedStream  
CoGroupedStreams  
JoinedStreams  
QueryableStateStream  
WindowedStream   
IntervalJoin: 在一个时间间隔内执行连接  
    - between  
    - ProcessJoinFunction
    - IntervalJoined
        - process
JoinedStreams:   



MapFunction  
FilterFunction  
KeyedProcessFunction  
ProcessFunction  
FlatMapFunction  
ReduceFunction   
FoldFunction   
WindowFunction:  apply    
AllWindowFunction:  
CoFlatMapFunction:   
OutputSelector: 将流 split 成需要的  


KeyBy
window  
windowAll  
union:   
internalJoin:   
coGroup:   
connect:

### 基础组件
JobManager  
TaskManager   
ResourceManager  
Dispatcher  

JobGraph  
StreamGraph  
ExecutionGraph   


createKeyedStateBackend
RocksDBStateBackend.
AbstractKeyedStateBackend

ListState: PartitionableListState  
BroadcastState:  HeapBroadcastState
- HeapBroadcastState 中通过 HashMap 来保存 state 数据：
    - state 数据被保存在一个由多层 java Map 嵌套而成的数据结构中
      StateTable 的数据结构, ，查找 key 对应的 StateMap:
      根据是否开启异步 checkpoint，StateMap 会分别对应两个实现类：CopyOnWriteStateMap<K, N, S> 和 NestedStateMap<K, N, S>。 对于 NestedStateMap，实际存储数据如下：
      CopyOnWriteStateMap 是一个支持 Copy-On-Write 的 StateMap 子类，实际上参考了 HashMap 的实现，它支持 渐进式哈希(incremental rehashing) 和异步快照特性。
      对于 RocksDBKeyedStateBackend，每个 state 存储在一个单独的 column family 内，KeyGroup、key、 namespace 进行序列化存储在 DB 作为 key，状态数据作为 value。



FlinkKafkaConsumerBase:  
- open
- run  
- createPartitionDiscover:创建用于为此子任务查找新分区的分区发现程序。
  参数1：topicsDescriptor : 描述我们是为固定主题还是主题模式发现分区,也就是ﬁxedTopics和topicPattern的封 装。其中ﬁxedTopics明确指定了topic的名称，称为固定topic。topicPattern为匹配topic名称的正则表达式，用于分 区发现。
  参数2：indexOfThisSubtask ：此consumer子任务的索引。   
  参数3：numParallelSubtasks : 并行consumer子任务的总数 
  方法返回一个分区发现器的实例
  
open  
  打开分区发现程序，初始化所有需要的Kafka连接。  

initailizeConnections():   
初始化所有需要的Kafka链接源码  

discoveryPartitions()    

assign():   返回应该分配给特定Kafka分区的目标子任务的索引   


将restoredState中保存的一组topic的partition和要开始读取的起始偏移量保存到 subscribedPartitionsToStartOﬀsets

其中restoredStateEntry.getKey为某个Topic的摸个partition,restoredStateEntry.getValue为该partition的要开始读 取的起始偏移量 过滤掉topic名称不符合topicsDescriptor的topicPattern的分区

直接启动consumer


StartupMode:   
该枚举类型有5个值：

GROUP_OFFSETS：从保存在zookeeper或者是Kafka broker的对应消费者组提交的oﬀset开始消费，这个是默 认的配置   
EARLIEST：尽可能从最早的oﬀset开始消费   
LATEST：从最近的oﬀset开始消费   
TIMESTAMP：从用户提供的timestamp处开始消费 
SPECIFIC_OFFSETS：从用户提供的oﬀset处开始消费

open方法源码： 
(1)指定oﬀset提交模式
OﬀsetCommitMode:

OﬀsetCommitMode:表示偏移量如何从外部提交回Kafka brokers/ Zookeeper的行为   
它的确切值是在运行时在使用者子任务中确定的。  
DISABLED：完全禁用oﬀset提交。   
ON_CHECKPOINTS：只有当检查点完成时，才将偏移量提交回Kafka。   
KAFKA_PERIODIC：使用内部Kafka客户端的自动提交功能，定期将偏移量提交回Kafka。  
使用多个配置值确定偏移量提交模式

如果启用了checkpoint，并且启用了checkpoint完成时提交oﬀset，返回ON_CHECKPOINTS。  
如果未启用checkpoint，但是启用了自动提交，返回KAFKA_PERIODIC。  
其他情况都返回DISABLED。  




run方法： (1) 判断保存分区和读取起始偏移量的集合是否为空：

(2) 记录Kafka oﬀset成功提交和失败提交的数量  
(3) 获取当前自任务的索引  

（4） 注册一个提交时的回调函数，提交成功时，提交成功计数器加一；提交失败时，提交失败计数器加一
（5） 接下来判断subscribedPartitionsToStartOﬀsets集合是否为空。如果为空，标记数据源的状态为暂时空闲。

（6）创建一个KafkaFetcher,借助KafkaConsumer API从Kafka的broker拉取数据
（7） 根据分区发现间隔时间，来确定是否启动分区定时发现任务 如果没有配置分区定时发现时间间隔，则直接启动获取数据任务；否则，启动定期分区发现任务和数据获取任务
循环拉取数据源码：
createAndStartDiscoveryLoop:启动分区发现任务的方法：
尝试发现新的分区：
将发现的新分区添加到kafkaFetcher中
启动分区发现定时任务
partitionDiscoverer.discoverPartitions()的调用，即发现分区的执行过程。

kafkaFetcher的runFetchLoop方法
此方法为FlinkKafkaConsumer获取数据的主入口，通过一个循环来不断获取kafka broker的数据。
KafkaConsumerThread线程的run方法实例化handover
回到KafkaFecher类中的runFetchLoop方法

partitionConsumerRecordsHandler方法

该方法包含的内容为 FlinkKafkaConsumer 首先设置提交oﬀset的模式。 接下来创建和启动分区发现工具。

subscribedPartitionsToStartOffsets

的初始化逻辑。

为已订阅的分区列表，这里将它初始化。




PipelineExecutor   
Dispatcher   


