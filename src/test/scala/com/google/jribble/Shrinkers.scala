package com.google.jribble

import org.scalacheck.Shrink
import Shrink._
import ast._

/**
 * Contains definitions of Shrink[T] for T being one of AST nodes.
 *
 * Shrink[T] is being used by ScalaCheck to find shrink T to smaller example still
 * falsifying tested property.
 *
 * Shrinks defined for recursive (even not directly) definitions can flatten the structure
 * in addition to just shrinking subtrees. See shrinkExpression for example of that technique.
 */
object Shrinkers {

  implicit def shrinkClassDef: Shrink[ClassDef] = Shrink { 
    case x@ClassDef(modifs, name, ext, implements, body) =>
      (for (v <- shrink(modifs)) yield x.copy(modifs = v)) append
      (for (v <- shrink(name)) yield x.copy(name = v)) append
      (for (v <- shrink(ext)) yield x.copy(ext = v)) append
      (for (v <- shrink(implements)) yield x.copy(implements = v)) append
      (for (v <- shrink(body)) yield x.copy(body = v))
  }

  implicit def shrinkInterfaceDef: Shrink[InterfaceDef] = Shrink {
    case x@InterfaceDef(modifs, name, ext, body) =>
      (for (v <- shrink(modifs)) yield x.copy(modifs = v)) append
      (for (v <- shrink(name)) yield x.copy(name = v)) append
      (for (v <- shrink(ext)) yield x.copy(ext = v)) append
      (for (v <- shrink(body)) yield x.copy(body = v))
  }

  implicit def shrinkParamDef: Shrink[ParamDef] = Shrink {
    case x@ParamDef(name, _) =>
      for (v <- shrink(name)) yield x.copy(name = v)
  }

  implicit def shrinkConstructor: Shrink[Constructor] = Shrink {
    case x@Constructor(name, params, body) =>
      (for (v <- shrinkName.shrink(name)) yield x.copy(name = v)) append
      (for (v <- shrink(params)) yield x.copy(params = v)) append
      (for (v <- shrink(body)) yield x.copy(body = v))
  }

  implicit def shrinkMethodDef: Shrink[MethodDef] = Shrink {
    case x@MethodDef(modifs, _, name, params, body) =>
      (for (v <- shrink(modifs)) yield x.copy(modifs = v)) append
      (for (v <- shrinkName.shrink(name)) yield x.copy(name = v)) append
      (for (v <- shrink(params)) yield x.copy(params = v)) append
      (for (v <- shrink(body)) yield x.copy(body = v))
  }

  implicit def shrinkBlock: Shrink[Block] = Shrink {
    case Block(statements) =>
      for (v <- shrink(statements)) yield Block(v)
  }

  implicit def shrinkSignature: Shrink[Signature] = Shrink {
    case x@Signature(on, name, paramTypes, _) =>
      (for (v <- shrink(on)) yield x.copy(on = v)) append
      (for (v <- shrinkName.shrink(name)) yield x.copy(name = v)) append
      (for (v <- shrink(paramTypes)) yield x.copy(paramTypes = v))
  }

  implicit def shrinkNewCall: Shrink[NewCall] = Shrink {
    case x@NewCall(signature, params) =>
      (for (v <- shrink(params)) yield x.copy(params = v)) append
      (for (v <- shrink(signature)) yield x.copy(signature = v))
  }

  implicit def shrinkVarRef: Shrink[VarRef] = Shrink {
    case VarRef(name) => for (v <- shrinkName.shrink(name)) yield VarRef(name)
  }

  implicit def shrinkExpression: Shrink[Expression] = Shrink {
    case x: VarRef => shrink(x)
    case ThisRef => shrink(ThisRef)
    case x@MethodCall(on, _, params) =>
      shrink(x) append
      shrink(on) append
      params.map(shrink(_)).foldLeft(Stream.empty[Expression])(interleave)      
    case x@StaticMethodCall(_, _, params) =>
      shrink(x) append
      params.map(shrink(_)).foldLeft(Stream.empty[Expression])(interleave)  
    case x@NewCall(_, params) =>
      shrink(x) append
      params.map(shrink(_)).foldLeft(Stream.empty[Expression])(interleave)
    case x: Literal => shrink(x)
    case x@Conditional(condition, _, then, elsee) =>
      shrink(x) append
      shrink(condition) append
      shrink(then) append
      shrink(elsee)
  }

  implicit def shrinkMethodCall: Shrink[MethodCall] = Shrink {
    case x@MethodCall(on, signature, params) =>
      (for (v <- shrink(on)) yield x.copy(on = v)) append
      (for (v <- shrink(signature)) yield x.copy(signature = v)) append
      (for (v <- shrink(params)) yield x.copy(params = v))
  }

  implicit def shrinkStaticMethodCall: Shrink[StaticMethodCall] = Shrink {
    case x@StaticMethodCall(classRef, signature, params) =>
      (for (v <- shrink(classRef)) yield x.copy(classRef = v)) append
      (for (v <- shrink(signature)) yield x.copy(signature = v)) append
      (for (v <- shrink(params)) yield x.copy(params = v))
  }

  implicit def shrinkIf: Shrink[If] = Shrink {
    case x@If(condition, then, elsee) =>
      (for (v <- shrink(condition)) yield x.copy(condition = v)) append
      (for (v <- shrink(then)) yield x.copy(then = v)) append
      (for (v <- shrink(elsee)) yield x.copy(elsee = v))
  }

  implicit def shrinkLiteral: Shrink[Literal] = Shrink { x: Literal =>
    x match {
      case StringLiteral(vv) => for (v <- shrink(vv)) yield StringLiteral(v)
      case DoubleLiteral(vv) => for (v <- shrink(vv)) yield DoubleLiteral(v)
      case FloatLiteral(vv)  => for (v <- shrink(vv)) yield FloatLiteral(v)
      case IntLiteral(vv)    => for (v <- shrink(vv)) yield IntLiteral(v)
      case LongLiteral(vv)   => for (v <- shrink(vv)) yield LongLiteral(v)
      //these literals cannot be shrunk so we return empty Stream
      case BooleanLiteral(_) | CharLiteral(_) | NullLiteral => Stream.empty
    }
  }

  private val shrinkName: Shrink[String] = Shrink { s =>
    shrink(s).filterNot(_.isEmpty)
  }

  /**
   * A method that merges to Streams by interleaving elements.
   *
   * This method has been copied from ScalaCheck library.
   */
  private def interleave[T](xs: Stream[T], ys: Stream[T]): Stream[T] =
    if(xs.isEmpty) ys
    else if(ys.isEmpty) xs
    else Stream(xs.head, ys.head) append interleave(xs.tail, ys.tail)

}