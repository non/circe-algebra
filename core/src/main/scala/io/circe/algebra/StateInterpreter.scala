package io.circe.algebra

import cats.MonadError
import cats.data.StateT

abstract class StateInterpreter[F[_], J](implicit M: MonadError[F, Failure]) extends Interpreter[F, J] { self =>
  import Op._

  type S[x] = StateT[F, J, x]

  def runS[A](s: StateT[F, J, A])(j: J): F[A] = s.runA(j)

  def apply[A](op: Op[A]): StateT[F, J, A] = op match {
    case ReadNull        => StateT.inspectF(readNull)
    case ReadBoolean     => StateT.inspectF(readBoolean)
    case ReadNumber      => StateT.inspectF(readNumber)
    case ReadLong        => StateT.inspectF(readLong)
    case ReadString      => StateT.inspectF(readString)
    case DownField(key)  => StateT.modifyF(downField(key))
    case DownAt(index)   => StateT.modifyF(downAt(index))
    case ReadFields(opA) => StateT.get[F, J].flatMapF(readFields(opA))
    case ReadValues(opA) => StateT.get[F, J].flatMapF(readValues(opA))

    case Pure(value)     => StateT.pure(value)
    case Fail(failure)   => StateT.lift(M.raiseError(failure))
    case Map(opA, f)     => self(opA).map(f)
    case Bind(opA, f)    => self(opA).flatMap(a => self(f(a)))
    case Join(opA, opB)  => self(opA).product(self(opB))

    case Bracket(opA)    => StateT.get[F, J].flatMap(j => self(opA).modify(_ => j))
  }
}
