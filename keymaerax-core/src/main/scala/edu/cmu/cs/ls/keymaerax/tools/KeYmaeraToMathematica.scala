/**
 * Copyright (c) Carnegie Mellon University. CONFIDENTIAL
 * See LICENSE.txt for the conditions of this license.
 */
/**
  * @note Code Review: 2016-08-02
  */
package edu.cmu.cs.ls.keymaerax.tools

// favoring immutable Seqs
import scala.collection.immutable._
import com.wolfram.jlink._
import edu.cmu.cs.ls.keymaerax.Configuration
import edu.cmu.cs.ls.keymaerax.btactics.FormulaTools
import edu.cmu.cs.ls.keymaerax.core._
import edu.cmu.cs.ls.keymaerax.tools.MathematicaConversion.{KExpr, MExpr}
import edu.cmu.cs.ls.keymaerax.tools.MathematicaNameConversion._

import scala.math.BigDecimal

/**
  * Converts KeYmaeara X [[edu.cmu.cs.ls.keymaerax.core.Expression expression data structures]]
  * to Mathematica Expr objects.
  * @author Stefan Mitsch
  * @author Nathan Fulton
 */
object KeYmaeraToMathematica extends KeYmaeraToMathematica
class KeYmaeraToMathematica extends K2MConverter[KExpr] {

  def m2k: M2KConverter[KExpr] = MathematicaToKeYmaera

  /**
   * Converts KeYmaera expressions into Mathematica expressions.
   */
  private[tools] def convert(e: KExpr): MExpr = {
    val convertInterpretedSymbols = Configuration.getOption(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS).getOrElse("false").toBoolean
    insist(convertInterpretedSymbols || StaticSemantics.symbols(e).forall({case Function(_, _, _, _, interpreted) => !interpreted case _ => true}),
      "Interpreted functions not allowed in soundness-critical conversion to Mathematica")
    insist(StaticSemantics.symbols(e).forall({case fn@Function(_, _, _, _, true) => interpretedSymbols.contains(fn) case _ => true}),
      "Interpreted functions must have expected domain and sort")
    insist(disjointNames(StaticSemantics.symbols(e)), "Disjoint names required for Mathematica conversion")

    e match {
      case t: Term => convertTerm(t)
      case f: Formula => convertFormula(f)
      case p: Program => throw new IllegalArgumentException("There is no conversion from hybrid programs to Mathematica " + e)
      case f: Function => throw new IllegalArgumentException("There is no conversion from unapplied function symbols to Mathematica " + e)
    }
  }

  private[tools] def disjointNames(symbols: Set[NamedSymbol]): Boolean = {
    val names = symbols.map(x=>(x.name,x.index)).toList
    names.distinct.length == names.length
  }

  /**
   * Converts a KeYmaera terms to a Mathematica expression.
   */
  protected[tools] def convertTerm(t : Term): MExpr = {
    require(t.sort == Real || t.sort == Unit || FormulaTools.sortsList(t.sort).forall(_ == Real), "Mathematica can only deal with reals not with sort " + t.sort)
    t match {
      //@todo Code Review: clean up FuncOf conversion into two cases here
      //@solution: inlined and simplified the FuncOf cases, moved uniform name conversion into MathematicaNameConversion
      //@solution: distinguish between interpreted and uninterpreted function symbols
      case FuncOf(fn, child) if fn.interpreted => convertFunction(interpretedSymbols(fn), child)
      //@note Uninterpreted functions are mapped to namespace kyx` to avoid clashes with any interpreted names
      case FuncOf(fn, child) if !fn.interpreted => convertFunction(toMathematica(fn), child)
      case Neg(c) => new MExpr(MathematicaSymbols.MINUSSIGN, Array[MExpr](convertTerm(c)))
      case Plus(l, r) => new MExpr(MathematicaSymbols.PLUS, Array[MExpr](convertTerm(l), convertTerm(r)))
      case Minus(l, r) => new MExpr(MathematicaSymbols.MINUS, Array[MExpr](convertTerm(l), convertTerm(r)))
      case Times(l, r) => new MExpr(MathematicaSymbols.MULT, Array[MExpr](convertTerm(l), convertTerm(r)))
      case Divide(l: Number, r: Number) if l.value.isValidLong && r.value.isValidLong =>
        new MExpr(MathematicaSymbols.RATIONAL, Array[MExpr](convertTerm(l), convertTerm(r)))
      case Divide(l, r) => new MExpr(MathematicaSymbols.DIV, Array[MExpr](convertTerm(l), convertTerm(r)))
      case Power(l, r) => new MExpr(MathematicaSymbols.EXP, Array[MExpr](convertTerm(l), convertTerm(r)))
      case Number(n) if n.isWhole => new MExpr(n.toBigIntExact().getOrElse(
        throw new ConversionException("Unexpected: whole BigDecimal cannot be converted to BigInteger")).bigInteger)
      case Number(n) if !n.isWhole && n.scale > 0 =>
        val num = BigDecimal(n.bigDecimal.unscaledValue())
        val denom = BigDecimal(BigDecimal(1).bigDecimal.movePointRight(n.scale))
        assert(n == num/denom, "Expected double to rational conversion to have value " + n + ", but got numerator " + num + " and denominator " + denom)
        new MExpr(MathematicaSymbols.RATIONAL, Array[MExpr](convert(Number(num)), convert(Number(denom))))
      case Number(n) if !n.isWhole && n.scale < 0 =>
        //@note negative scale means: unscaled*10^(-scale)
        val num = BigDecimal(n.bigDecimal.unscaledValue()).bigDecimal.movePointLeft(n.scale)
        assert(n == BigDecimal(num), "Expected double conversion to have value " + n + ", but got " + num)
        convert(Number(num))
      case Number(n) => throw new ConversionException("Number is neither BigInteger nor encodable as rational of BigInteger: " + n)
      case t: Variable => toMathematica(t)
      case Pair(l, r) =>
        //@note converts nested pairs into nested lists of length 2 each
        new MExpr(Expr.SYM_LIST, Array[MExpr](convertTerm(l), convertTerm(r)))
    }
  }

  /**
   * Converts KeYmaera formulas into Mathematica objects
   */
  protected def convertFormula(f : Formula): MExpr = f match {
    case And(l, r)  => new MExpr(MathematicaSymbols.AND, Array[MExpr](convertFormula(l), convertFormula(r)))
    case Equiv(l,r) => new MExpr(MathematicaSymbols.BIIMPL, Array[MExpr](convertFormula(l), convertFormula(r)))
    case Imply(l,r) => new MExpr(MathematicaSymbols.IMPL, Array[MExpr](convertFormula(l), convertFormula(r)))
    case Or(l, r)   => new MExpr(MathematicaSymbols.OR, Array[MExpr](convertFormula(l), convertFormula(r)))
    case Equal(l,r) => new MExpr(MathematicaSymbols.EQUALS, Array[MExpr](convertTerm(l), convertTerm(r)))
    case NotEqual(l,r) => new MExpr(MathematicaSymbols.UNEQUAL, Array[MExpr](convertTerm(l), convertTerm(r)))
    case LessEqual(l,r) => new MExpr(MathematicaSymbols.LESS_EQUALS, Array[MExpr](convertTerm(l), convertTerm(r)))
    case Less(l,r)   => new MExpr(MathematicaSymbols.LESS, Array[MExpr](convertTerm(l), convertTerm(r)))
    case GreaterEqual(l,r) => new MExpr(MathematicaSymbols.GREATER_EQUALS, Array[MExpr](convertTerm(l), convertTerm(r)))
    case Greater(l,r) => new MExpr(MathematicaSymbols.GREATER, Array[MExpr](convertTerm(l), convertTerm(r)))
    case False => MathematicaSymbols.FALSE
    case True => MathematicaSymbols.TRUE
    case Not(phi) => new MExpr(MathematicaSymbols.NOT, Array[MExpr](convertFormula(phi)))
    case exists: Exists => convertExists(exists)
    case forall: Forall => convertForall(forall)
    case _ => throw new ConversionException("Don't know how to convert " + f + " of class " + f.getClass)
  }

  /** Converts a universally quantified formula. */
  protected def convertForall(f: Quantified): MExpr = {
    /** Recursively collect universally quantified variables, return variables+child formula */
    def collectVars(vsSoFar: Seq[NamedSymbol], candidate: Formula): (Seq[NamedSymbol], Formula) = {
      candidate match {
        case Forall(nextVs, nextf) => collectVars(vsSoFar ++ nextVs, nextf)
        case _ => (vsSoFar, candidate)
      }
    }

    val (vars, formula) = collectVars(f.vars, f.child)
    val variables = new MExpr(MathematicaSymbols.LIST, vars.map(toMathematica).toArray)
    new MExpr(MathematicaSymbols.FORALL, Array[MExpr](variables, convertFormula(formula)))
  }

  /** Converts an existentially quantified formula. */
  protected def convertExists(f: Quantified): MExpr = {
    /** Recursively collect existentially quantified variables, return variables+child formula */
    def collectVars(vsSoFar: Seq[NamedSymbol], candidate: Formula): (Seq[NamedSymbol], Formula) = {
      candidate match {
        case Exists(nextVs, nextf) => collectVars(vsSoFar ++ nextVs, nextf)
        case _ => (vsSoFar, candidate)
      }
    }

    val (vars, formula) = collectVars(f.vars, f.child)
    val variables = new MExpr(MathematicaSymbols.LIST, vars.map(toMathematica).toArray)
    new MExpr(MathematicaSymbols.EXISTS, Array[MExpr](variables, convertFormula(formula)))
  }

  /** Convert a function. */
  private def convertFunction(head: MExpr, child: Term): MExpr = child match {
    case _: Pair =>
      assert(convertTerm(child).listQ(), "Converted pair expected to be a list, but was " + convertTerm(child))
      new MExpr(head, convertTerm(child).args())
    case Nothing => new MExpr(head, Array[MExpr]())
    case _ => new MExpr(head, Array[MExpr](convertTerm(child)))
  }
}
