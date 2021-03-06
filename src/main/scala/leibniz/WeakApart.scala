package leibniz

import leibniz.inhabitance.{Inhabited, Proposition, Uninhabited}
import leibniz.internal.Unsafe
import leibniz.variance.{Constant, Injective}

/**
  * In constructive mathematics, an apartness relation is a constructive
  * form of inequality, and is often taken to be more basic than equality.
  * It is often written as # to distinguish from the negation of equality
  * (the denial inequality) ≠, which is weaker.
  *
  * An apartness relation is a symmetric irreflexive binary relation with
  * the additional condition that if two elements are apart, then any other
  * element is apart from at least one of them (this last property is often
  * called co-transitivity or comparison).
  *
  * @see [[https://en.wikipedia.org/wiki/Apartness_relation
  *        Apartness relation]]
  */
sealed abstract class WeakApart[A, B] { nab =>
  import WeakApart._

  /**
    * If `F[A]` equals to `F[B]` for unequal types `A` and `B`,
    * then `F` must be a constant type constructor.
    */
  def proof[F[_]](f: F[A] === F[B]): Constant[F]

  /**
    * Having `A === B` and `A =!= B` at the same time leads to a contradiction.
    */
  def contradicts(ab: A === B): Void = {
    val id: Constant[λ[x => x]] = proof[λ[x => x]](ab)
    id.proof[Unit, Void].coerce(())
  }

  /**
    * Inequality is a co-transitive relation: if two elements
    * are apart, then any other element is apart from at least
    * one of them.
    */
  def compare[C]: Inhabited[Either[A =!= C, B =!= C]] = {
    val f: (A === C, B === C) => Void = (ac, bc) => nab.contradicts(ac andThen bc.flip)
    Inhabited.and(f).map {
      case Left(nac) => Left(witness(nac))
      case Right(nbc) => Right(witness(nbc))
    }
  }

  /**
    * Inequality is symmetric relation and therefore can be flipped around.
    * Flipping is its own inverse, so `x.flip.flip == x`.
    */
  def flip: B =!= A = new (B =!= A) {
    def proof[F[_]](f: F[B] === F[A]): Constant[F] =
      nab.proof[F](f.flip)

    override def flip: A =!= B = nab
  }

  /**
    * Strengthen the proof by providing explicit type descriptions.
    */
  def strengthen(implicit A: ConcreteType[A], B: ConcreteType[B]): Apart[A, B] =
    Apart.witness(this, A, B)

  /**
    * Given an injective [[F]], if `A ≠ B`, then `F[A] ≠ F[B]`.
    */
  def lift[F[_]](implicit F: Injective[F]): F[A] =!= F[B] =
    witness[F[A], F[B]](p => contradicts(F.proof(p)))
}
object WeakApart {
  private[this] final class Witness[A, B](nab: (A === B) => Void) extends WeakApart[A, B] {
    def proof[F[_]](f: F[A] === F[B]): Constant[F] =
      Constant.witness[F, A, B](this, f)
  }

  def apply[A, B](implicit ev: WeakApart[A, B]): WeakApart[A, B] = ev

  implicit def proposition[A, B]: Proposition[WeakApart[A, B]] =
    Proposition.force[WeakApart[A, B]](Unsafe.unsafe)

  implicit def inhabited[A, B](implicit A: Inhabited[A === B]): Uninhabited[A =!= B] =
    Uninhabited.witness(nab => A.contradicts(ab => nab.contradicts(ab)))

  implicit def uninhabited[A, B](implicit na: Uninhabited[A === B]): Inhabited[A =!= B] =
    Inhabited.value(witness(na.contradicts))

  implicit def mkWeakApart[A, B]: A =!= B =
    macro internal.MacroUtil.mkWeakApart[A, B]

  /**
    * Inequality is an irreflexive relation.
    */
  def irreflexive[A](ev: A =!= A): Void =
    ev.contradicts(Is.refl[A])

  def witness[A, B](f: (A === B) => Void): WeakApart[A, B] =
    new Witness[A, B](f)

  def force[A, B](implicit unsafe: Unsafe): WeakApart[A, B] =
    witness(unsafe.void[A === B])
}