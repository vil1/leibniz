package leibniz

import leibniz.inhabitance.Proposition
import leibniz.internal.Unsafe

/**
  * The data type `Is` is the encoding of Leibnitz’ law which states that
  * if `a` and `b` are identical then they must have identical properties.
  * Leibnitz’ original definition reads as follows:
  *   a ≡ b = ∀ f .f a ⇔ f b
  * and can be proven to be equivalent to:
  *   a ≡ b = ∀ f .f a → f b
  *
  * The `Is` data type encodes true type equality, since the identity
  * function is the only non-diverging conversion function that can be used
  * as an implementation of the `subst` method assuming that we do not break
  * parametricity. As the substitution function has to work for any `F[_]`, it
  * cannot make assumptions about the structure of `F[_]`, making it impossible
  * to construct a value of type `F[A]` or to access values of type `A` that
  * may be stored inside a value of type `F[A]`. Hence it is impossible for
  * a substitution function to alter the value it takes as argument.
  *
  * Not taking into account the partial functions that never terminate
  * (infinite loops), functions returning `null`, or throwing exceptions,
  * the identity function is the only function that can be used in place of
  * `subst` to construct a value of type `Is[A, B]`.
  *
  * The existence of a value of type `Is[A, B]` now implies that a ≡ b,
  * since the conversion function, that converts an `A` into a `B`, must be
  * the identity function.
  *
  * This technique was first used in
  * [[http://portal.acm.org/citation.cfm?id=583852.581494
  * Typing Dynamic Typing]] (Baars and Swierstra, ICFP 2002).
  *
  * @see [[===]] `A === B` is a type synonym to `Is[A, B]`
  * @see [[http://typelevel.org/blog/2014/09/20/higher_leibniz.html
  *        Higher Leibniz]]
  */
sealed abstract class Is[A, B] private[Is]()  { ab =>
  import Is._

  /**
    * To create an instance of `Is[A, B]` you must show that for every
    * choice of `F[_]` you can convert `F[A]` to `F[B]`.
    */
  def subst[F[_]](fa: F[A]): F[B]

  /**
    * Substitution on identity brings about a direct coercion function of the
    * same form that `=:=` provides.
    *
    * @see [[coerce]]
    */
  def apply(a: A): B = coerce(a)

  /**
    * Substitution on identity brings about a direct coercion function of the
    * same form that [[=:=]] provides.
    *
    * @see [[apply]]
    */
  def coerce(a: A): B = {
    type f[x] = x
    subst[f](a)
  }

  /**
    * Equality is transitive relation and its witnesses can be composed
    * in a chain much like functions.
    *
    * @see [[compose]]
    */
  final def andThen[C](bc: B === C): A === C = {
    type f[b] = A === b
    bc.subst[f](ab)
  }

  /**
    * Equality is transitive relation and its witnesses can be composed
    * in a chain much like functions.
    *
    * @see [[andThen]]
    */
  final def compose[Z](za: Z === A): Z === B =
    za.andThen(ab)

  /**
    * Equality is symmetric relation and therefore can be flipped around.
    * Flipping is its own inverse, so `x.flip.flip == x`.
    */
  final def flip: B === A = {
    type f[a] = a === A
    subst[f](refl)
  }


  /**
    * Given `A === B` we can prove that `F[A] === F[B]`.
    *
    * @see [[Is.lift]]
    * @see [[Is.lift2]]
    */
  final def lift[F[_]]: F[A] === F[B] =
    Is.lift[F, A, B](ab)

  /**
    * Given `A === B` and `I === J` we can prove that `F[A, I] === F[B, J]`.
    *
    * This method allows you to compose two `===` values in infix manner:
    * {{{
    *   def either(ab: A === B, ij: I === J): Either[A, I] === Either[B, J] =
    *     ab lift2[Either] ij
    * }}}
    *
    * @see [[Is.lift]]
    * @see [[Is.lift2]]
    * @see [[Is.lift3]]
    */
  def lift2[F[_, _]]: PartiallyAppliedLift2[F] =
    new PartiallyAppliedLift2[F]
  final class PartiallyAppliedLift2[F[_, _]] {
    def apply[I, J](ij: I === J): F[A, I] === F[B, J] =
      Is.lift2[F, A, B, I, J](ab, ij)
  }

  /**
    * Given `A === B` we can convert `(X => A)` into `(X => B)`.
    */
  def onF[X](fa: X => A): X => B = {
    type f[a] = X => a
    subst[f](fa)
  }

  /**
    * Given `A === B`, prove `A =:= B`.
    */
  def toPredef: A =:= B = {
    type f[a] = A =:= a
    subst[f](implicitly[A =:= A])
  }

  /**
    * Given `A === B`, make an `Iso[A, B]`.
    */
  def toIso: Iso[A, B] =
    subst[Iso[A, ?]](Iso.id[A])

  /**
    * Given `A === B`, prove `A <~< B`.
    */
  def toAs: A <~< B = {
    type f[a] = A <~< a
    subst[f](As.refl[A])
  }
}

object Is {
  implicit def proposition[A, B]: Proposition[Is[A, B]] =
    Proposition.force[Is[A, B]](Unsafe.unsafe)

  def apply[A, B](implicit ev: A Is B): A Is B = ev

  final case class Refl[A]() extends Is[A, A] {
    def subst[F[_]](fa: F[A]): F[A] = fa
  }

  /**
    * Equality is reflexive relation.
    */
  implicit def refl[A]: A === A = new Refl[A]()

  /**
    * Given `A === B` we can prove that `F[A] === F[B]`.
    *
    * @see [[lift2]]
    * @see [[lift3]]
    */
  def lift[F[_], A, B]
  (ab: A === B): F[A] === F[B] = {
    type f[α] = F[A] === F[α]
    ab.subst[f](refl)
  }


  /**
    * Given `A === B` and `I === J` we can prove that `F[A, I] === F[B, J]`.
    *
    * @see [[lift]]
    * @see [[lift3]]
    */
  def lift2[F[_, _], A, B, I, J]
  (ab: A === B, ij: I === J): F[A, I] === F[B, J] = {
    type f1[α] = F[A, I] === F[α, I]
    type f2[α] = F[A, I] === F[B, α]
    ij.subst[f2](ab.subst[f1](refl))
  }

  /**
    * Given `A === B`, `I === J`, and `M === N` we can prove that
    * `F[A, I] === F[B, J]`.
    *
    * @see [[lift]]
    * @see [[lift2]]
    */
  def lift3[F[_, _, _], A, B, I, J, M, N]
  (ab: A === B, ij: I === J, mn: M === N): F[A, I, M] === F[B, J, N] = {
    type f1[α] = F[A, I, M] === F[α, I, M]
    type f2[α] = F[A, I, M] === F[B, α, M]
    type f3[α] = F[A, I, M] === F[B, J, α]
    mn.subst[f3](ij.subst[f2](ab.subst[f1](refl)))
  }

  /**
    * It can be convenient to convert a [[=:=]] value into a `Leibniz` value.
    * This is not strictly valid as while it is almost certainly true that
    * `A =:= B` implies `A === B` it is not the case that you can create
    * evidence of `A === B` except via a coercion. Use responsibly.
    */
  def fromPredef[A, B](eq: A =:= B): A === B = Axioms.predefEq(eq)

  /**
    * Unsafe coercion between types. `force` abuses `asInstanceOf` to
    * explicitly coerce types. It is unsafe, but needed where Leibnizian
    * equality isn't sufficient.
    */
  def force[A, B](implicit unsafe: Unsafe): A === B =
    unsafe.coerceK2_1[===, A, B](refl[A])
}