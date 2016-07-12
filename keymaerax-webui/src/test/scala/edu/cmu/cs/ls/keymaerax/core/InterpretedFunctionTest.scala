/**
* Copyright (c) Carnegie Mellon University.
* See LICENSE.txt for the conditions of this license.
*/

package edu.cmu.cs.ls.keymaerax.core
import scala.collection.immutable._
import edu.cmu.cs.ls.keymaerax.btactics.{RandomFormula, TacticTestBase, TactixLibrary}
import testHelper.KeYmaeraXTestTags.{CheckinTest, SlowTest, SummaryTest, UsualTest}
import edu.cmu.cs.ls.keymaerax.parser.StringConverter._

/**
 * Interpreted functions / defined functions substitution tests.
 * @author Andre Platzer
 */
class InterpretedFunctionTest extends TacticTestBase {
  import TactixLibrary._

  "Interpreted / defined functions" should "not substitute abs" taggedAs(SummaryTest) in withMathematica { qeTool =>
    val wrong = "f(-1)=1".asFormula
    val intbase = "abs(-1)=1".asFormula
    val pr = proveBy(intbase, QE)
    pr shouldBe 'proved
    //@todo Either SubstitutionClashException or a result that isn't proved
    val pr2 = pr(USubst(SubstitutionPair(FuncOf(Function("abs",None,Real,Real),DotTerm), FuncOf(Function("f",None,Real,Real),DotTerm)) :: Nil))
    pr2.conclusion shouldBe Sequent(IndexedSeq(), IndexedSeq(wrong))
    pr2 should not be 'proved
  }

  it should "not prove false 0=1 via substitution of abs" in withMathematica { qeTool =>
    val wrong = "0=1".asFormula
    val intbase = "abs(-1)=1".asFormula
    val pr = proveBy(intbase, QE)
    pr shouldBe 'proved
    //@todo Either SubstitutionClashException or a result that isn't proved
    val pr2 = pr(USubst(SubstitutionPair(FuncOf(Function("abs",None,Real,Real),DotTerm), Number(0)) :: Nil))
    pr2.conclusion shouldBe Sequent(IndexedSeq(), IndexedSeq(wrong))
    pr2 should not be 'proved
  }

  //@todo similarly for min, max
}