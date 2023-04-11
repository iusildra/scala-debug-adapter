package ch.epfl.scala.debugadapter.internal

import scala.util.Try
import scala.jdk.CollectionConverters.*
import scala.util.Failure
import scala.util.Success

package object evaluator {
  implicit class SafeSeq[A](seq: Seq[Safe[A]]) {
    def traverse: Safe[Seq[A]] = {
      seq.foldRight(Safe(Seq.empty[A])) { (safeHead, safeTail) =>
        safeTail.flatMap(tail => safeHead.map(head => head +: tail))
      }
    }
  }

  implicit class TrySeq[A](seq: Seq[Try[A]]) {
    def traverse: Try[Seq[A]] = {
      seq.foldRight(Try(Seq.empty[A])) { (safeHead, safeTail) =>
        safeTail.flatMap(tail => safeHead.map(head => head +: tail))
      }
    }
  }

  implicit class SafeOption[A](opt: Option[Safe[A]]) {
    def traverse: Safe[Option[A]] =
      opt.map(s => s.map(Option.apply)).getOrElse(Safe(None))
  }

  implicit class TryToSafe[A](t: Try[A]) {
    def toSafe: Safe[A] = Safe(t)
    def toSeq: Seq[A] = t match {
      case Failure(exception) => Seq()
      case Success(value) => Seq(value)
    }
  }

  implicit class ValidationSeq[A](seq: Seq[Validation[A]]) {
    def traverse: Validation[Seq[A]] = {
      seq.foldRight(Validation(Seq.empty[A])) { (safeHead, safeTail) =>
        safeTail.flatMap(tail => safeHead.map(head => head +: tail))
      }
    }
  }

  implicit class SeqToJava[A](seq: Seq[A]) {
    def toValidation(message: String): Validation[A] =
      seq.size match {
        case 1 => Valid(seq.head)
        case 0 => Recoverable(message)
        case _ => Unrecoverable(s"$message: multiple values found")
      }

    @inline def asJavaList = seq.asJava
  }

  implicit class JavaListToScala[A](list: java.util.List[A]) {
    @inline def asScalaSeq: Seq[A] = list.asScala.toSeq
  }
}
