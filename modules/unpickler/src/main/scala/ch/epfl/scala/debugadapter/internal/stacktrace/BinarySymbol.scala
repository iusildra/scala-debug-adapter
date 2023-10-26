package ch.epfl.scala.debugadapter.internal.stacktrace

import tastyquery.Symbols.*
import tastyquery.Types.Type
import tastyquery.Types.TypeOrMethodic
import tastyquery.Contexts.Context

sealed trait BinarySymbol

sealed trait BinaryClassSymbol extends BinarySymbol:
  def symbol: Symbol
  override def toString: String =
    def format(sym: Symbol) =
      val span = sym.tree.map(tree => s"(${tree.pos.startLine}, ${tree.pos.endLine})").getOrElse("")
      s"$sym $span"
    this match
      case BinaryClass(sym) => s"BinaryClass(${format(sym)})"
      case BinarySyntheticCompanionClass(symbol) => s"BinarySyntheticCompanionClass(${format(symbol)})"
      case BinarySAMClass(sym, _, tpe) => s"BinarySAMClass(${format(sym)}, ${tpe.showBasic})"
      case BinaryPartialFunction(sym, tpe) => s"BinaryPartialFunction(${format(sym)}, ${tpe.showBasic})"

  def tastyClass: Option[ClassSymbol] = this match
    case BinaryClass(symbol) => Some(symbol)
    case _ => None

  def companionClass(using Context): Option[BinaryClass] = this match
    case BinaryClass(symbol) => symbol.companionClass.map(BinaryClass.apply)
    case BinarySyntheticCompanionClass(symbol) => Some(BinaryClass(symbol))
    case _: BinarySAMClass => None
    case _: BinaryPartialFunction => None

final case class BinaryClass(symbol: ClassSymbol) extends BinaryClassSymbol
final case class BinarySyntheticCompanionClass(symbol: ClassSymbol) extends BinaryClassSymbol
final case class BinarySAMClass(symbol: TermSymbol, parentClass: ClassSymbol, tpe: Type) extends BinaryClassSymbol
final case class BinaryPartialFunction(symbol: TermSymbol, tpe: Type) extends BinaryClassSymbol

sealed trait BinaryMethodSymbol extends BinarySymbol:
  def declaredType: TypeOrMethodic

final case class BinaryMethod(binaryOwner: BinaryClassSymbol, symbol: TermSymbol) extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = symbol.declaredType

final case class BinaryLocalLazyInit(binaryOwner: BinaryClassSymbol, symbol: TermSymbol) extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = symbol.declaredType

final case class BinaryLazyInit(binaryOwner: BinaryClassSymbol, symbol: TermSymbol) extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = symbol.declaredType

final case class BinaryTraitParamAccessor(binaryOwner: BinaryClassSymbol, symbol: TermSymbol)
    extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = symbol.declaredType

final case class BinaryMixinForwarder(binaryOwner: BinaryClassSymbol, target: BinaryMethodSymbol)
    extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = target.declaredType

final case class BinaryTraitStaticForwarder(binaryOwner: BinaryClassSymbol, symbol: TermSymbol)
    extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = symbol.declaredType
final case class BinaryOuter(binaryOwner: BinaryClassSymbol, declaredType: Type) extends BinaryMethodSymbol
final case class BinarySuperArg(binaryOwner: BinaryClassSymbol, init: TermSymbol, declaredType: Type)
    extends BinaryMethodSymbol
final case class BinaryLiftedTry(binaryOwner: BinaryClassSymbol, declaredType: Type) extends BinaryMethodSymbol
final case class BinaryByNameArg(binaryOwner: BinaryClassSymbol, declaredType: Type) extends BinaryMethodSymbol
final case class BinaryMethodBridge(target: BinaryMethodSymbol, declaredType: TypeOrMethodic) extends BinaryMethodSymbol
final case class BinaryAnonOverride(
    binaryOwner: BinaryClassSymbol,
    overriddenSymbol: TermSymbol,
    declaredType: TypeOrMethodic
) extends BinaryMethodSymbol
final case class BinaryStaticForwarder(
    binaryOwner: BinaryClassSymbol,
    target: BinaryMethodSymbol,
    declaredType: TypeOrMethodic
) extends BinaryMethodSymbol
final case class BinaryDeserializeLambda(binaryOnwer: BinaryClassSymbol, declaredType: TypeOrMethodic)
    extends BinaryMethodSymbol
final case class BinarySetter(binaryOwner: BinaryClassSymbol, symbol: TermSymbol, declaredType: TypeOrMethodic)
    extends BinaryMethodSymbol
final case class BinarySuperAccessor(binaryOwner: BinaryClassSymbol, sym: TermSymbol, declaredType: TypeOrMethodic)
    extends BinaryMethodSymbol

final case class BinarySpecializedMethod(binaryOwner: BinaryClassSymbol, symbol: TermSymbol) extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = symbol.declaredType

final case class BinaryInlineAccessor(target: BinaryMethodSymbol) extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = target.declaredType

final case class BinaryAdaptedFun(target: BinaryMethodSymbol) extends BinaryMethodSymbol:
  override def declaredType: TypeOrMethodic = target.declaredType
