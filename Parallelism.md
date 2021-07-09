## Tasks

```scala
val (v1, v2) = parallel(e1, e2)
```

We can write alternatively using the task construct:

```scala
val t1 = task(e1)
val t2 = task(e2)
val v1 = t1.join
val v2 = t2.join
```

t = task(e) starts computation e “in the background”

-  t is a task, which performs computation of e
-  current computation proceeds in parallel with t
-  to obtain the result of e, use t.join
-  t.join blocks and waits until the result is computed
-  subsequent t.join calls quickly return the same results

**Sigatures**

```scala
def parallel[A, B](taskA: => A, taskB: => B): (A, B) = { ... }

//minimal interface for tasks
def task[A](c: => A) : Task[A]

trait Task[A] {
    def join: A
}
```

Implementation of _parallel_ using _task_:

```scala
def parallel[A, B](cA: => A, cB: => B): (A, B) = {
    val tB: Task[B] = task { cB }
    val tA: A = cA
    (tA, tB.join)
}
```

## Parallel sorting

#### Merge sort

```scala
def parMergeSort(xs: Array[Int], maxDepth: Int): Unit {
    //intermediate array
    val ys = new Array[Int](xs.length)

    def sort(from: Int, until: Int, depth: Int): Unit = {
        if (depth == maxDepth) {
            quickSort(xs, from, until - from)
        } else {
            val mid = (from + until) / 2
            parallel(sort(mid, unitl, depth + 1), sort(from, mid, depth + 1))
            val flip = (maxDepth - depth) % 2 == 0
            val src = if (flip) ys else xs
            val dst = if (flip) xs else ys
            merge(src, dst, from, mid, until)
        }
    }
    sort(0, xs.length, 0)

    def merge(src: Array[Int], dst: Array[Int], from: Int, mid: Int, until: Int): Unit

    def copy(src: Array[Int], target: Array[Int]from: Int, until: Int, depth: Int): Unit {
        if (depth == maxDepth) {
            Array.copy(src, from, target, from, until - from)
        } else {
            val mid = (from + until) \ 2
            val right = parallel(
                copy(src, target, mid, until, depth + 1),
                copy(src, target, from, mid, depth + 1)
            )
        }
    }
    if (maxDepth % 2 == 0) copy(ys, xs, 0, xs.length, 0)
}
```

## Data Operations and Parallel Mapping, Reduce, Scan

#### Functional programming and collections

**map**

-  List(1, 3, 8).map(x => x\*x) == List(1, 9, 64)

**fold**

-  List(1, 3, 8).fold(100)((s, x) => s + x) == 112
-  List(1, 3, 8).foldLeft(100)((s, x) => s - x) == ((100 - 1) - 3) - 8 == 88
-  List(1, 3, 8).foldRight(100)((s, x) => s - x) == 1 - (3 - (8 - 100)) == -94

**reduce**

-  List(1, 3, 8).reduceLeft((s, x) => s - x) == (1 - 3) - 8 == -10
-  List(1, 3, 8).reduceRight((s, x) => s - x) == 1 - (3 - 8) == 6
   _Just need associativity_

**scan**

-  List(1, 3, 8).scan(100)((s, x) => s + x) == List(100, 101, 104, 112)

#### Associative operation
Operation $f: (A, A) => A$ is associative $\xLeftrightarrow{} f(x, f(y, z)) == f(f(x, y), z)$
or
$(x \otimes y)\otimes(z \otimes w) = (x \otimes (y \otimes z)) \otimes w = ((x \otimes y) \otimes z) \otimes w$

#### Commutative operation

Operation $f: (A, A) => A$ is associative $\xLeftrightarrow{} f(x, y) == f(y, x)$

#### Parallel map of an array producing an array

```scala
def mapASegPar[A, B](inp: Array[A], left: Int, right: Int, f: A => B, out: Array[B]): Unit = {
    if (right - left < threshold)
        mapASegSeq(inp, left, right, f, out)
    else {
        val mid = left + (right - left) / 2
        parallel(mapASegPar(inp, left, mid, f, out),
        parallel(mapASegPar(inp, mid, right, f, out))
    }
}
```

#### Parallel map on immutable trees

```scala
sealed abstract class Tree[A] { val size: Int}
case class Leaf[A](a: Array[A]) extends Tree[A] {
    override val size = l.size + r.size
}
case class Node[A](l: Tree[A], r: Tree[A]) extends Tree[A] {
    override val size = l.size + r.size
}
```

```scala
def mapTreePar[A: Manifest, B: Manifest](t: Tree[A], f: A=> B): Tree[B] = {
    t match {
        case Leaf(a) => {
            val len = a.length
            val b = new Array[B](len)
            var i = 0
            while (i < len) {
                b(i) = f(a(i))
                i = i + 1
            }
            Leaf(b)
        }
        case Node(l, r) => {
            val (lb, rb) = parallel(mapTreePar(l, f), mapTreePar(r, f))
            Node(lb, rb)
        }
    }
}
```

#### Parallel reduce of a tree

```scala
def reduce[A](t: Tree[A], f: (A, A) => A): A = {
    t match {
        case Leaf(v) => v
        case Node(l, r) =>
            val (lV, rV) = parallel(reduce[A](l, f), reduce[A](r, f))
    }
}
```

#### Parallel Scan of a tree

```scala
sealed abstract class TreeRes[A] { val res: A}
case class LeafRes[A](override val res: A) extends TreeRes[A]
case class NodeRes[A](l: TreeRes[A], override val res: A, r: TreeRes[A]) extends TreeRes[A]
```

```scala
def reduceRes[A](t: Tree[A], f: (A, A) => A): TreeRes[A] = {
    t match {
        case Leaf(v) => LeafRes(v)
        case Node(l, r) =>
            val (tL, tR) = (reduceRes(l, f), reduceRes(r, f))
            NodeRes(tL, f(tL.res, tR.res), tR)
    }
}

def upsweep[A](t: Tree[A], f: (A, A) => A): TreeRes[A] = {
    t match {
        case Leaf(v) => LeafRes(v)
        case Node(l, r) =>
            val (tL, tR) = parallel(upsweep(l, f), upsweep(r, f))
            NodeRes(tL, f(tL.res, tR.res), tR)
    }
}

def downsweep[A](t: TreeRes[A], a0: A, f: (A, A) => A): Tree[A] = {
    t match {
        case LeafRes(a) => Leaf(f(a0, a))
        case NodeRes(l, _, r) =>
            val (tL, tR) = parallel(downsweep[A](l, a0, f), downsweep[A](r, f(a0, l.res), f))
            Node(tL, tR)
    }
}

def scanLeft[A](t: Tree[A], a0: A, f: (A, A) => A): Tree[A] = {
    val tRes = upsweep(t, f)
    val scan1 = downsweep(tRes, a0, f)
    prepend(a0, scan1)
}

def prepend[A](x: A, t: Tree[A]): Tree[A] = {
    t match {
        case Leaf(v) => Node(Leaf(x), Leaf(v))
        case Node(l, r) => Node(prepend(x, l), r)
    }
}
```

#### Parallel Scan of a tree with arrays as nodes

```scala
sealed abstract class TreeResA[A] { val res: A}
case class Leaf[A](from: Int, to: Int, override val res: A) extends TreeResA[A]
case class Node[A](l: TreeResA[A], override val res: A, r: TreeResA[A]) extends TreeResA[A]
```

```scala
def reduceSeg1[A](inp: Array[A], left: Int, right: Int, a0: A, f: (A, A) => A): A = {
    var a = a0
    var i - left
    while (i < right) {
        a = f(a, inp(i))
        i = i + 1
    }
    a
}

def upsweep[A](inp: Array[A], from: Int, to: Int, f: (A, A) => A): TreeResA[A] = {
    if (to - from < threshold)
        Leaf(from, to, reduceSeg1(inp, from + 1, to, inp(from), f))
    else {
        val mid = from + (to - from) / 2
        val (tL, tR) = parallel(upsweep(inp, from, mid, f), upsweep(inp, mis, to f))
        Node(tL, f(tL.res, tR.res), tR)
    }
}

def downsweep[A](inp: Array[A], a0: A, f: (A, A) => A, t: TreeResA[A], out: Array[A]): Unit = {
    t match {
        case Leaf(a) => scanLeftSeg(inp, from, to, a0, f, out)
        case Node(l, _, r) =>
            val (_, _) = parallel(downsweep(inp, a0, f, l, out), downsweep(inp, f(a0, l.res), f, r, out))

    }
}

def scanLeftSeg[A](inp: Array[A], left: Int, right: Int, a0: A, f: (A, A) => A, out: Array[A]): Unit = {
    if (left < right) {
        var i = left
        var a = a0
        while (i < right) {
            a = f(a, inp(i))
            i = i + 1
            out(i) = a
        }
    }
}

def scanLeft[A](inp: Array[A], a0: A, f: (A, A) => A, out: Array[A]): Unit = {
    val t = upsweep(inp, 0, inp.length, f)
    downsweep(inp, a0, f, t, out)
    out(0) = a0
}
```

## Data-Parallel operation

#### The aggregate Operation

```scala
def aggregate[B](z: B)(f: (B, A) => B, g: (B, B) => B): B
```

##Conc-Trees

Trees are not good for parallelism unless they are balanced.

Conc Invariants:

-  A <> node can never contain Empty as its subtree
-  The level difference between left and right of a <> node is always 1 or less

```scala
sealed trait Conc[+T] {
    def level: Int
    def size: Int
    def left: Conc[T]
    def right: Conc[T]
}

case object Empty extends Conc[Nothing] {
    def level = 0
    def size = 0
}

class Single[T](val x: T) extends Conc[T] {
    def level = 0
    def size = 1
}

case class <>[T](left: Conc[T], right: Conc[T]) extends Conc[T] {
    val level = 1 + math.max(left.level, right.level)
    val size = left.size + right.size
}

case class Append[T](left: Conc[T], right: Conc[T]) extends Conc[T] {
    val level = 1 + math.max(left.level, right.level)
    val size = left.size + right.size
}

```

```scala
def <>(that: Conc[T]): Conc[T] = {
    if (this == Empty) that
    else if (that == empty) this
    else concat(this, that)
}

//Concat takes O(h1 - h2) time
def concat[T](xs: Conc[T], ys: Conc[T]): Conc[T] = {
    val diff = ys.level - xs.level
    if (diff >= -1 && diff <= 1) new <>(xs, ys)
    else if (diff < -1) {
        if (xs.left.level >= xs.right.level) {
            val nr = concat(xs.right, ys)
            new <>(xs.left, nr)
        } else {
            val nrr = concat(xs.right.right, ys)
            if (nrr.level == xs.level -3) {
                val nl = xs.left
                val nr = new <>(xs.right.left, nrr)
                new <>(nl, nr)
            } else {
                val nl = new <>(xs.left, xs.right.left)
                val nr = nrr
                new <>(nl, nr)
            }
        }
    }
}

def appendLeaf[T](xs: Conc[T], ys: Single[T]): Conc[T] = {
    xs match{
        case Empty => ys
        case xs: single[T] => new <>(xs, ys)
        case _ <> _ => new Append(xs, ys)
        case xs: Append[T] => append(xs, ys)
    }
}

@tailrec private def append[T](xs: Append[T], ys: Conc[T]): Conc[T] = {
    if (xs.right.level > ys.level) new Append(xs, ys)
    else {
        val zs = new <>(xs.right, ys)
        xs.left match {
            case ws @ Append(_, _) => append(ws, zs)
            case ws if ws.level <= zs.level => ws <> zs
            case ws => new Append(ws, zs)
        }
    }
}
```
