import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future

class CountingSemaphore(units: Int) {

    private val availableUnits = new AtomicReference(units)

    def lock() : Unit = {
        val oldAvailable = availableUnits.get()
        if (oldAvailable == 0 || !availableUnits.compareAndSet(oldAvailable, oldAvailable - 1)) then lock()
    }

    def unlock(): Unit = {
        val oldAvailable = availableUnits.get()
        if (!availableUnits.compareAndSet(oldAvailable, oldAvailable + 1)) then unlock()
    }
}

def sequence(fs: List[Future[Int]]): Future[List[Int]] =
    fs.foldRight(Future.successful(List.empty[Int])){
        case (f: Future[Int], list: Future[List[Int]]) => 
            f.flatMap(i => list.map(l => i :: l))
        
}
    
def countByBin(f: Future[List[Int]], computeBin: Int => Bin): Future[Map[Bin, Int]] ={
    f.map(l => l.groupBy(computeBin).map( { case(b, values) => (b -> values.size) }))
}

val asyncData = List(
magicGeneration(),
magicGeneration()
)

val histogram: Future[Map[Bin, Count]] =
    countByBin(sequence(asyncData), i => i%10)

// These are the messages sent by the Scrutineer
case object VoteIsOver
case class IssueUnderVote(val question: String)
// These are the messages sent by an AssemblyMember
case object ImHere
sealed trait VoteOption
case object Yes extends VoteOption
case object No extends VoteOption
case object Blank extends VoteOption
// Message related to the Timer
case object TimesUp
case class StartTimer(duration: Long)

object Quorum extends App {
    val system = ActorSystem("Quorum")
    val AssemblySize: Int = 153
    val VotingItem: IssueUnderVote = IssueUnderVote("raise the EPFL tuition fee")
    val scrutineer =
        system.actorOf(Props(new Scrutineer(AssemblySize, VotingItem)))
    val AssemblyMembers = (0 until AssemblySize).map{
        id => system.actorOf(Props(new AssemblyMember(scrutineer,
            (10000 * Math.random()).toInt)), name = s"Assembly_member_$id")
    }
}

class Timer extends Actor {
    override def receive: Receive = {
        case StartTimer(duration: Long) =>
            Thread.sleep(duration)
            context.sender() ! TimesUp
    }
}

class AssemblyMember(scrutineer: ActorRef, thinkDelay: Int) extends Actor {
    val timer = context.actorOf(Props(new Timer))

    

    def makeUpMind(): VoteOption = {
        Math.random() match {
            case rng if rng < 0.45 => Yes
            case rng if rng < 0.9 => No
            case _ => Blank
        }
    }   

    scrutineer ! ImHere

    override def receive = awaitIssue

    def awaitIssue = {
        case IssueUnderVote =>
            timer ! StartTimer(thinkDelay)
            context.become(awaitThinking)
    }

    def awaitThinking: Receive = {
        case TimesUp => 
            scrutineer ! makeUpMind()
            context.become(awaitIssue)
        case VoteIsOver =>
            context.become(awaitIssue)
    }

}


class Scrutineer(assemblySize: Int, votingItem: IssueUnderVote) extends Actor {
    val AbsoluteMajority = assemblySize/2 + 1
    val VoteDuration = 50000L
    val timer = context.actorOf(Props(new Timer))

    override def receive = waitingToStart()

    def waitingToStart(presentAssemblyMembers: Set[ActorRef] = Set()): Receive = {
        // Wait for the quorum to be met and start the voting process
        case ImHere =>
            val newSet = presentAssemblyMembers + sender
            if (newSet.size >= AbsoluteMajority) then
                newSet.foreach(a => a ! votingItem) 
                timer ! StartTimer
                context.become(voting(Set(), newSet, (0, 0)))
            else 
                context.become(waitingToStart(newSet))
    }

    def voting(alreadyVoted: Set[ActorRef], presentMembers: Set[ActorRef], yesNoVotes: (Int, Int)): Receive = {
        /* Take care of AssemblyMember arriving late
        * Make sure no one is voting twice by filtering
        * invalid received VoteOption in a separate case
        */
        case ImHere =>
            if !presentMembers.contains(sender) then 
                sender ! votingItem
                context.become(voting(alreadyVoted, presentMembers + sender, yesNoVotes))
        case v: VoteOption if !alreadyVoted(sender) =>
            val (yesVotes, noVotes) = yesNoVotes
            val votes = v match {
                case Blank => yesNoVotes
                case Yes => (yesVotes + 1, noVotes)
                case No => (yesVotes, noVotes + 1)
            }
            val voteCasted = alreadyVoted + context.sender()
            tallyVotes(votes, voteCasted, presentMembers, presentMembers == voteCasted){ () =>
                context.become(voting(voteCasted, presentMembers, votes))
            } 
        case TimesUp =>
            tallyVotes(yesNoVotes, alreadyVoted, presentMembers, isOver = true)(() => {})
    }

    def done(): Receive = {
        case _ => println(s"The voting session is over, it is too late to vote " + context.sender())
    }

    /* Sets the scrutineer’s Receive to done() if the voting can be decided,
    and calls the callback cont() otherwise. */
    def tallyVotes(votes: (Int, Int), alreadyVoted: Set[ActorRef], members: Set[ActorRef], isOver: Boolean)(cont: () => Unit): Unit = {
        lazy val IssueUnderVote(question) = votingItem
        val (yesVotes, noVotes) = votes
        def endVote(message: String): Unit = {
            (members -- alreadyVoted) foreach(_ ! VoteIsOver)
            println(message)
            context.become(done())
        }
        if (yesVotes >= AbsoluteMajority)
            endVote(s"The Assembly agreed to $question by an absolute majority!")
        else if (noVotes >= AbsoluteMajority)
            endVote(s"The Assembly refused to $question by an absolute majority!")
        else if (isOver) {
            if (yesVotes > noVotes)
                endVote(s"The Assembly agreed to $question: $yesVotes against $noVotes votes!")
            else if (yesVotes == noVotes)
                endVote(s"The Assembly was unable to choose whether to $question or not")
            else
                endVote(s"The Assembly refused to $question: $noVotes against $yesVotes votes!")
        } else cont()
    }
}


val characters = sc.parallelize(List(
    Character(1, "Alek", None, 60, Warrior),
    Character(2, "Beatrix", Some(17), 45, Priest),
    Character(3, "Cleop", Some(17), 1, Mage),
    Character(4, "Deadra", Some(41), 60, Priest)))

val numberOfLevel60 = characters.filter(c => c.level == 60).count 

val averageLevelPerRole: Map[Role, Double] =
    characters.map(c => (c.role, (1, c.level))).reduceByKey(((c1, l1), (c2, l2)) => (c1 + c2, l1, l2)).mapValues((c, l) => l/c)

val mostNumerousGuildId: Int = 
    characters.map({case(Character(i, n, Some(guildId), l, r)) => (guildId, 1)}).reduceByKey((v1, v2) => v1 + v2).fold((0, -1))(((gid1, c1), (gid2, c2)) => if c2 >= c1 then (gid2, c2) else (gid1, c1))._1

val bossEvents = sc.parallelize(List(
    FightCompleted(1, false),
    FightCompleted(7, false),
    FightCompleted(4, true)))

val bossDefeatedRatio: Map[Role, Double] =
    characters.map(c => (c.id, c.role)).join(bossEvents).map((id, (role, won)) => (role, (if won then 1 else 0, 1))).reduceByKey(((w1, c1), (w2, c2)) => (w1 + w2, c1 + c2)).mapValues((w, c) => w.toDouble / c)