package ch.epfl.scala.debugadapter.internal.evaluator

import ch.epfl.scala.debugadapter.Logger
import scala.meta.{Type => _, _}

trait RuntimeValidator {
  def frame: JdiFrame
  def logger: Logger

  def validate(expression: String): Validation[RuntimeEvaluableTree]

  def validate(expression: Stat): Validation[RuntimeEvaluableTree]

  def validateBlock(block: Term.Block): Validation[RuntimeEvaluableTree]

  def validateLiteral(lit: Lit): Validation[RuntimeEvaluableTree]

  def validateName(value: String, methodFirst: Boolean): Validation[RuntimeEvaluableTree]

  def validateMethod(call: Call): Validation[RuntimeEvaluableTree]

  def validateSelect(select: Term.Select): Validation[RuntimeEvaluableTree]

  def validateNew(newValue: Term.New): Validation[RuntimeEvaluableTree]

  def validateOuter(tree: RuntimeTree): Validation[RuntimeEvaluableTree]

  def validateIf(tree: Term.If): Validation[RuntimeEvaluableTree]

  def validateAssign(tree: Term.Assign): Validation[RuntimeEvaluableTree]
}

trait RuntimeEvaluator {
  def frame: JdiFrame
  def logger: Logger

  /**
   * Evaluates an expression. Recursively evaluates its sub-expressions
   *
   * The debugger encapsulate variables in references. The returned value must be dereferenced to get the actual value
   *
   * @param stat
   * @return a [[Safe[JdiValue]]] of the result of the evaluation
   */
  def evaluate(stat: RuntimeEvaluableTree): Safe[JdiValue]

  def evaluateLiteral(tree: LiteralTree): Safe[JdiValue]

  def evaluateField(tree: InstanceFieldTree): Safe[JdiValue]

  def evaluateStaticField(tree: StaticFieldTree): Safe[JdiValue]

  def invokeStatic(tree: StaticMethodTree): Safe[JdiValue]

  def invokePrimitive(tree: PrimitiveBinaryOpTree): Safe[JdiValue]

  def invokePrimitive(tree: PrimitiveUnaryOpTree): Safe[JdiValue]

  def invoke(tree: InstanceMethodTree): Safe[JdiValue]

  def evaluateModule(tree: ModuleTree): Safe[JdiValue]

  def instantiate(tree: NewInstanceTree): Safe[JdiObject]

  def evaluateArrayElement(tree: ArrayElemTree): Safe[JdiValue]

  def evaluateIf(tree: IfTree): Safe[JdiValue]

  def evaluateAssign(tree: AssignTree): Safe[JdiValue]
}
