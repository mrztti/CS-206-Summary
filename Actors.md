##Why Actors?
CPUs are not getting faster anymore, they are getting wider:

-  multiple execution cores within one chip, sharing memory
-  virtual cores sharing a single physical execution core

Programs running on the computer must feed these cores:

-  running multiple programs in parallel (multi-tasking)
-  running parts of the same program in parallel (multi-threading)

**We want Non-Blocking Objects**:

-  blocking synchronization introduces dead-locks
-  blocking is bad for CPU utilization
-  synchronous communication couples sender and receiver

#The Actor Model
The Actor Model represents objects and their interactions, resembling
human organizations and built upon the laws of physics.
An Actor

-  is an object with identity
-  has a behavior
-  only interacts using asynchronous message passing

The Actor type describes the behavior, the execution is done by its
ActorContext.

```scala

type Receive = PartialFunction[Any, Unit]

trait Actor {
    implicit val context: ActorContext
    implicit val self: ActorRef
    def receive: Receive
    def sender: ActorRef
    ...
}

abstract class ActorRef {
    def !(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit
    def tell(msg: Any, sender: ActorRef) = this.!(msg)(sender)
    ...
}

trait ActorContext {
    def become(behavior: Receive, discardOld: Boolean = true): Unit
    def unbecome(): Unit
    def actorOf(p: Props, name: String): ActorRef
    def stop(a: ActorRef): Unit
...
}
```

####Actor Encapsulation
No direct access is possible to the actor behavior.
Only messages can be sent to known addresses (ActorRef):

-  every actor knows its own address (self)
-  creating an actor returns its address
-  addresses can be sent within messages (e.g. sender)

Actors are completely independent agents of computation:

-  local execution, no notion of global synchronization
-  all actors run fully concurrently
-  message-passing primitive is one-way communication

An actor is effectively single-threaded:

-  messages are received sequentially
-  behavior change is effective before processing the next message
-  processing one message is the atomic unit of execution

This has the benefits of synchronized methods, but blocking is replaced by enqueueing a message.

######Example counter

```scala
class Counter extends Actor {
    var count = 0
    def receive = {
        case ”incr” => count += 1
        case (”get”, customer: ActorRef) => customer ! count
    }
}

// OR

class Counter extends Actor {
var count = 0
    def receive = {
        case ”incr” => count += 1
        case ”get” => sender ! count
    }
}

// Changing an Actor's behavior

class Counter extends Actor {
    def counter(n: Int): Receive = {
        case ”incr” => context.become(counter(n + 1))
        case ”get” => sender ! n
    }
    def receive = counter(0)
}
```

######Example bank account

```scala
object BankAccount {
    case class Deposit(amount: BigInt) {
        require(amount > 0)
    }

    case class Withdraw(amount: BigInt) {
        require(amount > 0)
    }

    case object Done

    case object Failed
}

class BankAccount extends Actor {
    import BankAccount._

    var balance = BigInt(0)

    def receive = {
        case Deposit(amount) =>
            balance += amount
            sender ! Done
        case Withdraw(amount) if amount <= balance =>
            balance -= amount
            sender ! Done
        case _ => sender ! Failed
    }
}

```

```scala

object WireTransfer {
    case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
    case object Done
    case object Failed
}

class WireTransfer extends Actor {
    import WireTransfer._

    def receive = {
        case Transfer(from, to, amount) =>
            from ! BankAccount.Withdraw(amount)
            context.become(awaitWithdraw(to, amount, sender))
    }

    def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive = {
        case BankAccount.Done =>
            to ! BankAccount.Deposit(amount)
            context.become(awaitDeposit(client))
        case BankAccount.Failed =>
            client ! Failed
            context.stop(self)
        }
    }

    def awaitDeposit(client: ActorRef): Receive = {
        case BankAccount.Done =>
            client ! Done
            context.stop(self)
}
```
