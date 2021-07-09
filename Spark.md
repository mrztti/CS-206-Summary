# Spark

### Shared memory data parallelism

Shared memory data parallelism:

-  Split the data.
-  Workers/threads independently operate on the data shards in parallel.
-  Combine when done (if necessary)

### Distributed memory data parallelism

Distributed data parallelism:

-  Split the data over several nodes.
-  Nodes independently operate on the data shards in parallel.
-  Combine when done ( if necessary).

**New concern:** _Now we have to worry about network latency between workers._

**Shared memory case**: Data-parallel programming model. Data partitioned in memory and operated upon in parallel.

**Distributed case**: Data-parallel programming model. Data partitioned between machines, network in between, operated upon in parallel.

#### RDDs

Spark implements a distributed data parallel model called _Resilient Distributed Datasets_ (RDDs)

**Idea:** Keep all data immutable and in-memory. All operations on data are just functional transformations, like regular Scala collections. Fault tolerance is achieved by replaying functional transformations over original dataset.

```scala
abstract class RDD[TJ {
    def map[U](f: T => U): RDD[UJ = ...
    def flatMap[UJ(f: T => TraversableOnce[UJ): RDD[UJ = ...
    def filter(f: T => Boolean): RDD[TJ = ...
    def reduce(f: (T, T) => T): T = ...
}
```

RDDs can be created in two ways:

-  Transforming an existing RDD
-  From a _SparkContext_ (or _SparkSession_) object.
   The _SparkContext_ object (renamed _SparkSession_) can be thought of as your handle to the Spark cluster. It represents the connection between the Spark cluster and your running a pplication. It defines a handful of methods which can be used to create and populate a new RDD - parallelize: convert a local Scala collection to an RDD - textFile: read a text file from HD FS or a local file system and return an RDD of String

### Transformations and actions on Scala collection
Recall transformers and accessors from Scala sequential and parallel
collections.

**Transformers** Return new collections as results (Not single values)
_Examples: map, filter, flatMap, groupBy_

```scala
map(f: A=> B): Traversable[B]
```

**Accessors** Return single values as results (Not collections)
_Examples: reduce, fold, aggregate_

```scala
reduce(op: (A, A)=> A): A
```

### Transformations and actions on Spark

Similarly, Spark defines **transformations** and **actions** on RDDs.

**Transformations** Return new collections RDDs as results.
They are _lazy_, their result RDD is not immediately computed.

-  _**map[B](f: A=> B): RDD[B]**_
   Apply function to each element in the RDD and
   retrun an ROD of the result.
-  _**flatMap[B](f: A=> TraversableOnce[B]): RDD[B]**_
   Apply a function to each element in the RDD and return
   an ROD of the contents of the iterators returned.
-  _**filter(pred: A=> Boolean): RDD[A]**_
   Apply predicate function to each element in the RDD and
   return an ROD of elements that have passed the predicate
   condition, _pred_.
-  _**distinct(): RDD[B]**_
   Return RDD with duplicates removed.

**Actions** Compute a result based on an RDD, and either returned or
saved to an external storage system (e.g., HDFS).
They are _eager_, their result is immediately computed.

-  _**collect(): Array[T]**_
   Return all elements from RDD.

-  _**count(): Long**_
   Return the number of elements in the RDD.

-  _**take(num: Int): Array[T]**_
   Return the first num elements of the RDD

-  _**reduce(op: (A, A) => A): A**_
   Combine the elements in the RDD together using op
   function and return result.

-  _**foreach(f: T => Unit): Unit**_
   Apply function to each element in the RDD.

#### Caching and Persistence
By default, RDDs are recompted each time you run an action on them. This can be expensive if you need to use dataset more than once.

Spark allows you to control what is cached in memory by calling _persist()_ or _cache()_

### Pair RDDs
**Transformations**

-  groupByKey
-  reduceByKey
-  mapValues
-  keys
-  join
-  leftOuterJoin/rightOuterJoin

**Action**

-  countByKey

#### Joins

Joins are another sort of transformation on Pair RDDs. They're used to combine multiple datasets They are one of the most commonly-used operations on Pair RDDs!

There are two kinds of joins:

-  Inner joins (join)
-  Outer joins (leftOuterJoin/rightOuterJoin)

The key difference between the two is what happens to the keys when
both RDDs don't contain the same key.

**For example**, if I were to join two RDDs containing different customerIDs (the key), the difference between inner/outer joins is what happens to customers whose IDs don't exist in both RDDs.

Let's pretend the CFF has two datasets. One RDD representing customers and their subscriptions (abos), and another representing customers and cities they frequently travel to (locations). (E.g., gathered from CFF smartphone app)
How do we combine only customers that have a subscription and where there is location info?

```scala
val abos = ... // RDD[(Int, (String, Abonnement))]
val locations= ... // RDD[(Int, String)]

abos =
    (101, ("Ruetli", AG)),
    (102, ("Brelaz", DemiTarif)),
    (103, ("Gress", DemiTarifVisa)),
    (104, ("Schatten", DemiTarif))

locations =
    (101, "Bern"),
    (101, "Thun"),
    (102, "Lausanne"),
    (102, "Geneve"),
    (102, "Nyon"),
    (103, "Zurich"),
    (103, "St-Gallen"),
    (103, "Chur")
```

```scala
val trackedCustomers = abos.join(locations) // trackedCustomers: RDD[(Int, ((String, Abonnement) , String))]

trackedCustomers =
    (101, ((Ruetli, AG), Bern))
    (101, ((Ruetli, AG), Thun))
    (102, ((Brelaz, DemiTarif), Nyon))
    (102, ((Brelaz, DemiTarif), Lausanne))
    (102, ((Brelaz, DemiTarif), Geneve))
    (103, ((Gress, DemiTarifVisa), St-Gallen))
    (103, ((Gress, DemiTarifVisa), Chur))
    (103, ((Gress, DemiTarifVisa), Zurich))
```

```scala
val abosWithOptionalLocations = abos.leftOuterJoin(locations)

abosWithOptionalLocations =
    (101,((Ruetli,AG),Some(Thun)))
    (101,((Ruetli,AG),Some(Bern)))
    (102,((Brelaz,DemiTarif),Some(Geneve)))
    (102,((Brelaz,DemiTarif),Some(Nyon)))
    (102,((Brelaz,DemiTarif),Some(Lausanne)))
    (103,((Gress,DemiTarifVisa),Some(Zurich)))
    (103,((Gress,DemiTarifVisa),Some(St-Ga llen)))
    (103,((Gress,DemiTarifVisa),Some(Chur)))
    (104,((Schatten,DemiTarif),None))
```

```scala
val customersWithlocationDataAndOptionalAbos = abos.rightOuterJoin(locations)
// RDD[(Int, (Option[(String, Abonnement)J, String))]

customersWithlocationDataAndOptionalAbos =
    (101 ,(Some((Ruetli,AG)),Bern))
    (101 ,(Some((Ruetli,AG)),Thun))
    (102,(Some((Brelaz,DemiTarif)),Lausanne))
    (102,(Some((Brelaz,DemiTarif)),Geneve))
    (102,(Some((Brelaz,DemiTarif)),Nyon))
    (103,(Some((Gress,DemiTarifVisa)),Zurich))
    (103,(Some((Gress,DemiTarifVisa)),St-Gallen))
    (103,(Some((Gress,DemiTarifVisa)),Chur))

```

## Shuffling

Think again what happens when you have to do a groupBy or a groupByKey.
Remember our data is distributed! Did you notice anything odd?

```scala
val pairs = sc.parallelize(List((1, "one"), (2, "two"), (3, "three")))
pairs.groupByKey()
// res2: org.apache.spark.rdd.RDD[(Int, Iterable[String])]
//      = ShuffledRDD[16] at groupByKey at <console>:37
```

We typically have to move data from one node to another to beb _grouped with_ its key. Doing this is called **_shuffling_**.

##### Grouping and Reducing
Goal: calculate how many trips, and how much money was spent by each individual customer over the course of the month.

```scala
val purchasesRdd: RDD[CFFPurchaseJ = sc.textFile( ... )
val purchasesPerMonth =
    purchasesRdd.map(p => (p.customerId, (1, p.price))) // Pair RDD
                .reduceByKey((v1, v2) => (v1._1 + v2._1, v1._2 + v2._2))
                .collect()

```

**_By reducing the dataset first, the amount of data sent over the network during the shuffle is greatly redduced._**

#### Partitioning

Grouping all values of key-value pairs with the same key requires collecting all key-value pairs with the same key on the same machine

The data within an RDD is split into several partitions.
**Properties of partitions:**

-  Partitions never span multiple machines, i. e., tuples in the same partition are guaranteed to be on the same machine
-  Each machine in the cluster contains one or more partitions
-  The number of partitions to use is configura ble. By default, it equals _the total number of cores on all executor nodes_.

**Two kinds of partitioning available in Spark:**

-  Hash partitioning
   _Hash partitioning attempts to spread data evenly across
   partitions based on the key_
-  Range partitioning
   Pair RDDs may contain keys that have an ordering defined. For such RDDs, range partitioning may be more efficient. Using a range partitioner, keys are partitioned according to: - an ordering for keys - a set of sorted ranges of keys
   _Property: tuples with keys in the same range appear on the same machine._
