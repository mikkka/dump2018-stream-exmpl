package name.mtkachev.streamexmpl.gen

trait RNG {
  def next: (Long, RNG)
}

object RNG {
  case class SimpleRNG(seed: Long) extends RNG {
    def next: (Long, RNG) = {
      val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
      val nextRNG = SimpleRNG(newSeed)
      val n = (newSeed >>> 16)
      (n, nextRNG)
    }
  }

  type Rand[+A] = RNG => (A, RNG)

  val long: Rand[Long] = _.next

  def unit[A](a: A): Rand[A] =
    rng => (a, rng)

  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }

  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a, r1) = f(rng)
      g(a)(r1) // We pass the new state along
    }
}

object RNGSyntax {
  import name.mtkachev.streamexmpl.gen.RNG.Rand

  implicit class Ops[A](self: Rand[A]) {
    def map[B](f: A => B): Rand[B] = RNG.map(self)(f)

    def flatMap[B](g: A => Rand[B]): Rand[B] = RNG.flatMap(self)(g)
  }
}

object RNGS {
  import RNG.Rand
  import RNGSyntax._

  def intTuple(rnd: Rand[Int]): Rand[(Int, Int)] = {
    for {
      x <- rnd
      y <- rnd
    } yield (x, y)
  }
}
