package leibniz

import leibniz.inhabitance.Proposition
import leibniz.internal.Unsafe

/**
  * These are some non-trivial axioms that the library uses.
  */
object Axioms {
  type TypeConstructorParametricity <: Null
  type ClassicalPropositions <: Null
  type StandardLibrary <: Null
  type BoundedEquality <: Null
  type SubtypeBracket <: Null
  type BoundSquashing <: Null

  /**
    *
    */
  def tcIntersection[F[_], A, B]: F[A with B] === (F[A] with F[B]) =
    Is.force[F[A with B], (F[A] with F[B])](Unsafe.unsafe)

  /**
    * ∀ a b x y. (f a = f b) ∧ ¬(a = b) => f x = f y
    */
  def tcParametricity[F[_], A, B, X, Y](fab: F[A] === F[B], ab: (A === B) => Void): F[X] === F[Y] =
    Unsafe.unsafe.coerceK2_1[Is, F[X], F[Y]](fab)

  /**
    * ∀ a b x y. (f a = f b) ∧ ¬(f x = f y) => a = b
    */
  def tcInjectivity[F[_], A, B, X, Y](fab: F[A] === F[B], fxy: (F[X] === F[Y]) => Void): A === B =
    Unsafe.unsafe.coerceK2_1[Is, A, B](fab)

  /**
    * (a < b) ∧ (f a <= f b) => ∀ x y. (x <= y) => (f x <= f y)
    */
  def cotcParametricity[F[_], A, B, X, Y]
  (ab: (A === B) => Void, p: A <~< B, q: F[A] <~< F[B], r: X <~< Y): F[X] <~< F[Y] =
    Unsafe.unsafe.coerceK2_1[As, F[X], F[Y]](q)

  /**
    * (∀ x . f x = g x) => f = g
    */
  def tcExtensionality[F[_], G[_]]: TCExtensionality[F, G] = new TCExtensionality[F, G]

  final class TCExtensionality[F[_], G[_]](val b: Boolean = true) extends AnyVal {
    type T
    def apply(uv: F[T] === G[T]): F =~= G =
      Unsafe.unsafe.coerceK4_8[=~=, F, G](IsK.refl[F])

    def applyT(f: TypeHolder[T] => (F[T] === G[T])): F =~= G =
      apply(f(TypeHolder[T]))
  }

  /**
    *
    */
  def predefEq[A, B](eq: A =:= B): A === B =
    Unsafe.unsafe.coerceK2_1[Is, A, B](Is.refl[A])

  /**
    *
    */
  def predefConformity[A, B](eq: A <:< B): A <~< B =
    Unsafe.unsafe.coerceK2_1[As, A, B](As.refl[A])

  /**
    *
    */
  def fBounded[F[X <: F[X]], A <: F[A], B <: F[B], G[X <: F[X]]](eq: A === B, fa: G[A]): G[B] =
    Unsafe.unsafe.coerceK0[G[B]](fa)

  /**
    *
    */
  def bounded[L, H >: L, A >: L <: H, B >: L <: H, F[_ >: L <: H]](eq: A === B, fa: F[A]): F[B] =
    Unsafe.unsafe.coerceK0[F[B]](fa)

  /**
    * Subtyping is antisymmetric in theory (and in Dotty). Notice that this is
    * not true in Scala until [[https://issues.scala-lang.org/browse/SI-7278
    * SI-7278]] is fixed.
    */
  def bracket[A, B](f: A <~< B, g: B <~< A)(implicit unsafe: Unsafe): A === B =
    unsafe.coerceK2_1[Is, A, B](Is.refl[A])

  /**
    * Take two equal types `A === B` with different bounds `A >: LA <: HA`, `B >: LB <: HB`
    * and find a new type `C === A === B` that is bounded by `C >: (LA | LB) <: (HA & HB)`.
    *
    * Due to Scala2's lack of unions, the signature is a bit uglier than it could be.
    */
  def squash[
    LA, HA >: LA, A >: LA <: HA,
    LB >: LA <: HA, HB >: LB, B >: LB <: HB
  ] (eq: A === B): Squash[LA, HA, A, LB, HB, B] =
    Unsafe.unsafe.coerceK0[Squash[LA, HA, A, LB, HB, B]](Squash.refl[LA, HA, A])
}
