## Threads

The thread notation starts a new _thread_ – a concurrent execution.

```scala
thread {
    a = true
    y = if b then 0 else 1
}
```

The thread function is implemented as follows:

```scala
def thread(body: => Unit): Thread =
    val t = new Thread:
        override def run() = body
    t.start()
    t
```

The call t.join() lets the calling thread wait until thread t has terminated.

## Synchronization

```scala
object GetUID extends Monitor:
    var uidCount = 0
    def getUniqueId() = synchronized {
        val freshUID = uidCount + 1
        uidCount = freshUID
        freshUID
    }
```

In Scala, synchronized is a member method of AnyRef. In the call:

```scala
obj.synchronized { block }
```

obj serves as a lock.

-  block can be executed only by thread t holds the lock.
-  At most one thread can hold a lock at any one time.

Consequently, if another thread is already running synchronized on the
same object (i.e. it holds the lock), then a thread calling synchronized gets temporarily blocked, until the lock is released.

```scala
class Account(val name: String, initialBalance: Int) extends Monitor:
    private var myBalance = initialBalance

    def balance: Int = this.synchronized { myBalance }

    def add(n: Int): Unit = this.synchronized {
        myBalance += n
        if n > 10 then logTransfer(name, n)
    }
    val getUID = ThreadsGetUID.getUniqueId()

    def transfer(a: Account, b: Account, n: Int) =
        def adjust() { a.add(n); b.add(-n) }
        if a.getUID < b.getUID then
            a.synchronized { b.synchronized { adjust() } }
        else
            b.synchronized { a.synchronized { adjust() } }
```

A monitor can be used for more than just locking. Every monitor object
has the following methods:

-  **wait()** suspends the current thread,
-  **notify()** wakes up one other thread waiting on the current object,
-  **notifyAll()** wakes up all other thread waiting on the current object.

Note: these methods can only be called from inside a synchronized
statement.

```scala
class OnePlaceBuffer[Elem] extends Monitor:
    var elem: Elem = uninitialized; var full = false
    def put(e: Elem): Unit = synchronized {
        while full do wait()
        elem = e; full = true;
        notifyAll()
    }
    def get(): Elem = synchronized {
        while !full do wait()
        full = false; notifyAll(); elem
    }
```

#### Memory models

###### The Sequential Consistency Model
Consider all the reads and writes to program variables. If the result
of the execution is the same as if the read and write operations
were executed in some sequential order, and the operations of each
individual processor appear in the program order, then the model
is sequentially consistent.

###### The Java Memory Model
The Java Memory Model (JMM) defines a “happens-before” relationship
as follows.

-  Program order: Each action in a thread happens-before every
   subsequent action in the same thread.
-  Monitor locking: Unlocking a monitor happens-before every
   subsequent locking of that monitor.
-  Volatile fields: A write to a volatile field happens-before every
   subsequent read of that field.
-  Thread start: A call to start() on a thread happens-before all
   actions of that thread.
-  Thread termination. An action in a thread happens-before another
   thread completes a join on that thread.
-  Transitivity. If A happens before B and B happens-before C, then A
   happens-before C.

#### Volatile Variables

There’s a cheaper solution than using synchronized – we can use a _volatile field_.

Making a variable @volatile has several effects.

-  First, reads and writes to volatile variables are never reordered by the compiler.
-  Second, volatile reads and writes are never cached in CPU registers – they go directly to the main memory.
-  Third, writes to normal variables that in the program precede a volatile write W cannot be moved by the compiler after W.
-  Fourth, reads from normal variables that in the program appear after a volatile read R cannot be moved by the compiler before R.
-  Fifth, before a volatile write, values cached in registers must be written back to main memory.
-  Sixth, after a volatile read, values cached in registers must be re-read from the main memory.

## Executors
Thread creation is expensive.
Therefore, one often multiplexes threads to perform different tasks.
The JVM offers abstractions for that: ThreadPools and Executors.

-  Threads become in essence the workhorses that perform various tasks presented to them.
-  The number of available threads in a pool is typically some
   polynomial of the number of cores $N$. (e.g., $N^2$)
-  That number is independent of the number of concurrent activities to
   perform.

```scala
package concurrent
import scala.concurrent._
@main def SimpleExecute =
    execute{ log(”This task is run asynchronously.” }
    Thread.sleep(500)
```

#### Atomic variables
An atomic variable is a memory location that supports linearizable
operations.
A linearizable operation is one that appears instantaneously with the rest of the system. We also say the operation is performed atomically.
Classes that create atomic variables are defined in package

```scala
java.util.concurrent.atomic
```

They include

```scala
AtomicInteger, AtomicLong, AtomicBoolean, AtomicReference
```

```scala
import java.util.concurrent.atomic._
@main def AtomicUid =
    private val uid = new AtomicLong(0L)
    def getUniqueId(): Long = uid.incrementAndGet()
    execute {
    log(s”Got a unique id asynchronously: ${getUniqueId()}”)
    }
    log(s”Got a unique id: ${getUniqueId()}”)
```

```scala
class AtomicLong {
    ...
    def compareAndSet(expect: Long, update: Long) = this.synchronized {
        if this.get == expect then { this.set(update); true }
        else false
    }
```

```scala
@tailrec def getUniqueId(): Long =
    val oldUid = uid.get
    val newUid = oldUid + 1
    if uid.compareAndSet(oldUid, newUid) then newUid
    else getUniqueId()
```

###### Lazy Values

```scala
private var x_evaluating = false
private var x_defined = false
private var x_cached = uninitialized

def x: T =
    if !x_defined then
        this.synchronized {
            if x_evaluating then wait() else x_evaluating = true
        }
    if !x_defined then
        x_cached = E
        this.synchronized {
            x_evaluating = false; x_defined = true; notifyAll()
        }
    x_cached
```

###### Concurrent Queue implentation
Sequential:

```scala
object SeqQueue:
    private class Node[T](var next: Node[T]):
        var elem: T = uninitialized

class SeqQueue[T]:
    import SeqQueue._
    private var last = new Node[T](null)
    private var head = last

final def append(elem: T): Unit =
    val last1 = new Node[T](null)
    last1.elem = elem
    val prev = last
    last = last1
    prev.next = last1

final def remove(): Option[T] =
    if head eq last then None
    else
        val first = head.next
        head = first
        Some(first.elem)
```

Concurrent:

```scala
import java.util.concurrent.atomic._
import scala.annotation.tailrec

object ConcQueue:
    private class Node[T](@volatile var next: Node[T]):
        var elem: T = uninitialized

class ConcQueue[T]:
    import ConcQueue._
    private var last = new AtomicReference(new Node[T](null))
    private var head = new AtomicReference(last.get)

@tailrec final def append(elem: T): Unit =
    val last1 = new Node[T](null)
    last1.elem = elem
    val prev = last.get
    if last.compareAndSet(prev, last1)
    then prev.next = last1
    else append(elem)

@tailrec final def remove(): Option[T] =
    if head eq last then None
    else
    val hd = head.get
    val first = hd.next
    if first != null && head.compareAndSet(hd, first)
    then Some(first.elem)
    else remove()
```

# Futures
### From Continuation Passing Style to Future

```scala
def program(a: A, k: B => Unit): Unit
// Let’s massage this type signature…
// by currying the continuation parameter
def program(a: A): (B => Unit) => Unit
// by introducing a type alias
type Future[+T] = (T => Unit) => Unit
def program(a: A): Future[B]
// bonus: adding failure handling
type Future[+T] = (Try[T] => Unit) => Unit
// by reifying the alias into a proper trait
trait Future[+T] extends ((Try[T] => Unit) => Unit):
    def apply(k: Try[T] => Unit): Unit
// by renaming ‘apply‘ to ‘onComplete‘
trait Future[+T]:
    def onComplete(k: Try[T] => Unit): Unit
```

#### Simplified API of Future

```scala
trait Future[+A]:
    def onComplete(k: Try[A] => Unit)(using ExecutionContext): Unit
    // transform successful results
    def map[B](f: A => B): Future[B]
    def flatMap[B](f: A => Future[B]): Future[B]
    def zip[B](fb: Future[B]): Future[(A, B)]
    // transform failures
    def recover(f: Exception => A): Future[A]
    def recoverWith(f: Exception => Future[A]): Future[A]
```
