package edu.cmu.cs.ls.keymaerax.btactics

import java.io.{File, FileWriter, FilenameFilter}
import java.lang.reflect.InvocationTargetException

import edu.cmu.cs.ls.keymaerax.Configuration
import edu.cmu.cs.ls.keymaerax.bellerophon.BelleProvable
import edu.cmu.cs.ls.keymaerax.btactics.DerivedAxioms._
import edu.cmu.cs.ls.keymaerax.core.{Lemma, Sequent}
import edu.cmu.cs.ls.keymaerax.lemma.LemmaDBFactory
import edu.cmu.cs.ls.keymaerax.tags.{CheckinTest, IgnoreInBuildTest, SummaryTest, UsualTest}
import testHelper.KeYmaeraXTestTags
import testHelper.KeYmaeraXTestTags.OptimisticTest
import edu.cmu.cs.ls.keymaerax.parser.StringConverter._
import edu.cmu.cs.ls.keymaerax.pt.ProvableSig

import scala.collection.immutable
import scala.collection.immutable.Map
import scala.reflect.runtime.{universe => ru}
import scala.util.Try

/**
 * Tests [[edu.cmu.cs.ls.keymaerax.btactics.DerivedAxioms]]
  * @see [[CodeNameChecker]]
 */
@CheckinTest
@SummaryTest
@UsualTest
@IgnoreInBuildTest // otherwise it deletes derived lemmas while other tests are running
class DerivedAxiomsTests extends edu.cmu.cs.ls.keymaerax.btactics.TacticTestBase {

  private def check(lemma: Lemma): Sequent = {
    println(lemma.name.get + "\n" + lemma.fact.conclusion)
    lemma.fact shouldBe 'proved
    useToClose(lemma)
    lemma.fact.conclusion
  }

  private def useToClose(lemma: Lemma): Unit = {
    ProvableSig.startProof(lemma.fact.conclusion)(lemma.fact, 0) shouldBe 'proved
    //@note same test as previous line, just to make sure the lemma can be used by substitution
    theInterpreter(TactixLibrary.byUS(lemma), BelleProvable(ProvableSig.startProof(lemma.fact.conclusion))) match {
      case BelleProvable(provable, _) => provable shouldBe 'proved
      case _ => fail()
    }
  }

  "Derived axioms and rules" should "prove one-by-one on a fresh lemma database" taggedAs KeYmaeraXTestTags.CheckinTest in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      getDerivedAxiomsMirrors.foreach({ case (name, fm) =>
        // delete all stored lemmas
        LemmaDBFactory.lemmaDB.deleteDatabase()
        // re-initialize DerivedAxioms singleton object to forget lazy vals of previous iterations
        val c = DerivedAxioms.getClass.getDeclaredConstructor()
        c.setAccessible(true)
        withClue("Missing dependency in '" + name + "': inspect stack trace for occurrences of DerivedAxioms.scala for hints where to add missing dependency\n") {
          try {
            fm.bind(c.newInstance())()
          } catch {
            case ex: InvocationTargetException =>
              val missingDependency = "Lemma ([^\\s]*) should".r.findFirstMatchIn(ex.getCause.getMessage).
                map(_.group(1)).getOrElse("<unknown>")
              fail("Missing dependency to '" + missingDependency + "'", ex.getCause)
          }
        }
      })
    }
  }

  "Derived Rule" should "prove allG" in withMathematica { _ => allGeneralize.fact.subgoals shouldBe List(
    Sequent(immutable.IndexedSeq(), immutable.IndexedSeq("p_(||)".asFormula))
  ) }

  it should "prove Goedel" in withMathematica { _ => Goedel.fact.subgoals shouldBe List(
    Sequent(immutable.IndexedSeq(), immutable.IndexedSeq("p_(||)".asFormula))
  ) }

  it should "prove CT" in withMathematica { _ => CTtermCongruence.fact.subgoals shouldBe List(
    Sequent(immutable.IndexedSeq(), immutable.IndexedSeq("f_(||) = g_(||)".asFormula))
  ) }

  it should "prove [] monotone" in withMathematica { _ => boxMonotone.fact.subgoals shouldBe List(
    Sequent(immutable.IndexedSeq("p_(||)".asFormula), immutable.IndexedSeq("q_(||)".asFormula))
  ) }

  it should "prove [] monotone 2" in withMathematica { _ => boxMonotone2.fact.subgoals shouldBe List(
    Sequent(immutable.IndexedSeq("q_(||)".asFormula), immutable.IndexedSeq("p_(||)".asFormula))
  ) }

  it should "prove con convergence flat" in withMathematica { _ => convergenceFlat.fact.subgoals shouldBe List(
    //Sequent(immutable.IndexedSeq("v_<=0".asFormula, "J(||)".asFormula), immutable.IndexedSeq("p_(||)".asFormula)),
    Sequent(immutable.IndexedSeq("\\exists x_ (x_<=0 & J(||))".asFormula), immutable.IndexedSeq("p_(||)".asFormula)),
    Sequent(immutable.IndexedSeq("x_>0".asFormula, "J(||)".asFormula), immutable.IndexedSeq("<a_{|x_|};><x_:=x_-1;>J(||)".asFormula))
  ) }

  "Derived Axioms" should "prove <-> reflexive" in {check(equivReflexiveAxiom)}
  it should "prove !!" in {check(doubleNegationAxiom)}
  it should "prove exists dual" in {check(existsDualAxiom)}
  it should "prove all eliminate" taggedAs OptimisticTest ignore {check(allEliminateAxiom)}
  it should "prove exists eliminate" in {check(existsEliminate)}
  it should "prove exists eliminate y" in {check(existsEliminatey)}
  it should "prove !exists" in {check(notExists)}
  it should "prove !all" in {check(notAll)}
//  it should "prove !all2" in {check(notAll2)}
  it should "prove ![]" in {check(notBox)}
  it should "prove !<>" in {check(notDiamond)}
  it should "prove all distribute" in {check(allDistributeAxiom)}
  it should "prove all instantiate" in {check(allInstantiate)}
  it should "prove all then exists" in {check(allThenExists)}
  it should "prove all distribute elim" in {check(allDistributeElim)}
  it should "prove equiv expand" in {check(equivExpand)}
  it should "prove box dual" in {check(boxAxiom)}
  it should "prove V vacuous" in {check(vacuousAxiom)}
  it should "prove vacuous all quantifier" in {check(vacuousAllAxiom)}
//  it should "prove K1" in {check(K1)}
//  it should "prove K2" in {check(K2)}
  //@todo nrf it should "prove box split" in {check(boxAnd)}
//  it should "prove box split left" in {check(boxSplitLeft)}
//  it should "prove box split right" in {check(boxSplitRight)}
  it should "prove [] split" in {check(boxAnd)}
  it should "prove [] conditional split" in {check(boxImpliesAnd)}
  it should "prove <> split" in {check(diamondOr)}
  it should "prove []~><> propagation" in {check{boxDiamondPropagation}}
  it should "prove <:=> assign" in {check(assigndAxiom)}
//  it should "prove <:=> assign v" in {check(dummyassigndVvariant)}
  it should "prove := assign dual" in {check(assignDualAxiom)}
  it should "prove all substitute" in withMathematica { _ => check(allSubstitute)}
  it should "prove [:=] equational" in {check(assignbEquationalAxiom)}
  it should "prove [:=] assign equality exists" in {check(assignbExistsAxiom)}
  it should "prove exists and" in {check(existsAndAxiom)}
  it should "prove [:=] assign implies exists" in {check(assignbImpliesExistsAxiom)}
  it should "prove <:=> assign equality" in {check(assigndEqualityAxiom)}
  it should "prove <:=> assign dual 2" in {check(assignDual2Axiom)}
  it should "prove <:=> assign equality all" in {check(assigndEqualityAllAxiom)}
  it should "prove [:=] vacuous assign" in {check(vacuousAssignbAxiom)}
  it should "prove <:=> vacuous assign" in {check(vacuousAssigndAxiom)}
  //@todo it should "prove [':=] differential assign" in {check(assignDAxiomb)}
  it should "prove <':=> differential assign" in {check(assignDAxiom)}
  it should "prove <:*> assign nondet" in {check(nondetassigndAxiom)}
  it should "prove [y':=] differential assign 2" in {check(assignDAxiomby)}
  it should "prove <?> test" in {check(testdAxiom)}
  it should "prove <++> choice" in {check(choicedAxiom)}
  it should "prove <;> compose" in {check(composedAxiom)}
  it should "prove <*> iterate" in {check(iteratedAxiom)}
  it should "prove <*> approx" in {check(loopApproxd)}
  it should "prove [*] approx" in {check(loopApproxb)}
  it should "prove II induction" in {check(iiinduction)}
  it should "prove [*] merge" in {check(loopMergeb)}
  it should "prove <*> merge" in {check(loopMerged)}
  it should "prove [**] iterate iterate" in {check(iterateiterateb)}
  it should "prove <**> iterate iterate" in {check(iterateiterated)}
  it should "prove [*-] backiterate sufficiency" in {check(backiteratebsuff)}
  it should "prove [*-] backiterate necessity" in {check(backiteratebnecc)}
  it should "prove [*-] backiterate" in {check(backiterateb)}
  it should "prove Ieq induction" in {check(Ieq)}
  it should "prove [d] dual" in {check(dualbAxiom)}
  it should "prove [d] dual direct" in {check(dualbDirectAxiom)}
  it should "prove <d> dual direct" in {check(dualdDirectAxiom)}
  it should "prove exists generalize" in {check(existsGeneralize)}
  it should "prove vacuous exists" in {check(vacuousExistsAxiom)}
  it should "prove V[:*] vacuous assign nondet" in {check(vacuousBoxAssignNondetAxiom)}
  it should "prove V<:*> vacuous assign nondet" in {check(vacuousDiamondAssignNondetAxiom)}
  it should "prove & commute" in {check(andCommute)}
  it should "prove & assoc" in {check(andAssoc)}
  it should "prove !& deMorgan" in {check(notAnd)}
  it should "prove !| deMorgan" in {check(notOr)}
  it should "prove !-> deMorgan" in {check(notImply)}
  it should "prove !<-> deMorgan" in {check(notEquiv)}
  it should "prove -> converse" in {check(converseImply)}
  it should "prove domain commute" in {check(domainCommute)}
  it should "prove -> expand" in {check(implyExpand)}
  it should "prove Kd diamond modus ponens" in {check(KdAxiom)}
  it should "prove Kd2 diamond modus ponens" in {check(Kd2Axiom)}
  it should "prove PC1" in {check(PC1)}
  it should "prove PC2" in {check(PC2)}
  it should "prove PC3" in {check(PC3)}
  it should "prove -> tautology" in {check{implyTautology}}
  it should "prove ->'" in {check(Dimply)}
  it should "prove \\forall->\\exists" in {check(forallThenExistsAxiom)}
  //it should "prove DI differential invariance from DI" in {check(DIinvariance)}
  it should "prove DI differential invariant from DI" in {check(DIinvariant)}
  it should "prove DIo open differential invariance <" in withMathematica { _ => check(DIOpeninvariantLess)}
  it should "prove DV differential variant <=" in withMathematica {_ => check(DVLessEqual)}
  it should "prove DW differential weakening" in {check(DWeakening)}
  it should "prove DW differential weakening and" in {check(DWeakeningAnd)}
  it should "prove DR differential refine" in {check(DiffRefine)}
  it should "prove DC differential cut" in {check(DiffCut)}
  it should "prove DS no domain" in {check(DSnodomain)}
  it should "prove Dsol& differential equation solution" in {check(DSddomain)}
  it should "prove DGd differential ghost" in {check(DGddifferentialghost)}
  it should "prove DGCd diamond differential ghost const" in {check(DGCddifferentialghostconst)}
  it should "prove DGCd diamond differential ghost const exists" in {check(DGCddifferentialghostconstexists)}
  it should "prove DCd diamond differential cut" in {check(DCddifferentialcut)}
  it should "prove DWd diamond differential weakening" in {check(DWddifferentialweakening)}
  it should "prove DWd2 diamond differential weakening" in {check(DWd2differentialweakening)}
  it should "prove comma commute diamond" in {check(commaCommuted)}
  it should "prove DGd diamond inverse differential ghost implicational" in {check(DGdinversedifferentialghostimplicational)}
  //  it should "prove x' derive var" in {check(Dvar)}
  it should "prove x' derive variable" in {check(Dvariable)}
  it should "prove x' derive var commuted" in withMathematica { _ => check(DvariableCommuted)}
  it should "prove 'linear" in withMathematica { _ => check(Dlinear)}
  it should "prove 'linear right" in withMathematica { _ => check(DlinearRight)}
  it should "prove Uniq uniqueness iff" in {check(uniquenessIff)}
  it should "prove DG differential pre-ghost" in {check(DGpreghost)}
  it should "prove DX diamond differential skip" in {check(Dskipd)}
  it should "prove DBX>" in withMathematica {_ => check(darbouxGt)}
  it should "prove DBX> open" in withMathematica {_ => check(darbouxOpenGt)}
  it should "prove 0*" in withMathematica { _ => check(zeroTimes)}
  it should "prove 0+" in withMathematica { _ => check(zeroPlus)}
  it should "prove +0" in withMathematica { _ => check(plusZero)}
  it should "prove *0" in withMathematica { _ => check(timesZero)}
  it should "prove = reflexive" in withMathematica {_ =>check(equalReflex)}
  it should "prove = commute" in withMathematica { _ =>check(equalCommute)}
  it should "prove <=" in withMathematica { _ =>check(lessEqual)}
  it should "prove ! !=" in withMathematica { _ =>check(notNotEqual)}
  it should "prove >= flip" in withMathematica { _ =>check(flipGreaterEqual)}
  it should "prove > flip" in withMathematica { _ =>check(flipGreater)}
  it should "prove <= flip" in withMathematica { _ =>check(flipLessEqual)}
  it should "prove < flip" in withMathematica { _ =>check(flipLess)}
  it should "prove + associative" in withMathematica { _ => check(plusAssociative)}
  it should "prove * associative" in withMathematica { _ => check(timesAssociative)}
  it should "prove + commute" in withMathematica { _ => check(plusCommutative)}
  it should "prove * commute" in withMathematica { _ => check(timesCommutative)}
  it should "prove distributive" in withMathematica { _ => check(distributive)}
  it should "prove + identity" in withMathematica { _ => check(plusIdentity)}
  it should "prove * identity" in withMathematica { _ => check(timesIdentity)}
  it should "prove + inverse" in withMathematica { _ => check(plusInverse)}
  it should "prove * inverse" in withMathematica { _ => check(timesInverse)}
  it should "prove positivity" in withMathematica { _ => check(positivity)}
  it should "prove + closed" in withMathematica { _ => check(plusClosed)}
  it should "prove * closed" in withMathematica { _ => check(timesClosed)}
  it should "prove <" in withMathematica { _ => check(less)}
  it should "prove ! <" in withMathematica { _ => check(notLess)}
  it should "prove ! <=" in withMathematica { _ => check(notLessEqual)}
  it should "prove >" in withMathematica { _ => check(greater)}
  it should "prove ! >" in withMathematica { _ => check(notGreater)}
  it should "prove ! >=" in withMathematica { _ => check(notGreaterEqual)}

//  it should "prove != elimination" in withMathematica { _ => check(notEqualElim)}
//  it should "prove >= elimination" in withMathematica { _ => check(greaterEqualElim)}
//  it should "prove > elimination" in withMathematica { _ => check(greaterElim)}
  it should "prove 1>0" in withMathematica { _ => check(oneGreaterZero)}
  it should "prove nonnegative squares" in withMathematica { _ => check(nonnegativeSquares)}
  it should "prove >2!=" in withMathematica { _ => check(greaterImpliesNotEqual)}
  it should "prove > monotone" in withMathematica { _ => check(greaterMonotone)}

  it should "prove abs" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) { check(absDef) }
  }
  it should "prove min" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) { check(minDef) }
  }
  it should "prove max" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) { check(maxDef) }
  }
  it should "prove & recusor" in withMathematica { _ => check(andRecursor)}
  it should "prove | recursor" in withMathematica { _ => check(orRecursor)}
  it should "prove <= both" in withMathematica { _ => check(intervalLEBoth)}
  it should "prove < both" in withMathematica { _ => check(intervalLBoth)}
  it should "prove neg<= up" in withMathematica { _ => check(intervalUpNeg)}
  it should "prove abs<= up" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      check(intervalUpAbs)
    }
  }
  it should "prove max<= up" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      check(intervalUpMax)
    }
  }
  it should "prove min<= up" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      check(intervalUpMin)
    }
  }
  it should "prove +<= up" in withMathematica { _ => check(intervalUpPlus)}
  it should "prove -<= up" in withMathematica { _ => check(intervalUpMinus)}
  it should "prove *<= up" in withMathematica { _ => check(intervalUpTimes)}
  it should "prove pow<= up" in withMathematica { _ => check(intervalUpPower)}
  it should "prove 1Div<= up" in withMathematica { _ => check(intervalUp1Divide)}
  it should "prove <=+ down" in withMathematica { _ => check(intervalDownPlus)}
  it should "prove <=- down" in withMathematica { _ => check(intervalDownMinus)}
  it should "prove <=* down" in withMathematica { _ => check(intervalDownTimes)}
  it should "prove <=pow down" in withMathematica { _ => check(intervalDownPower)}
  it should "prove <=1Div down" in withMathematica { _ => check(intervalDown1Divide)}
  it should "prove K& down" in withMathematica { _ => check(Kand)}
  it should "prove &-> down" in withMathematica { _ => check(andImplies)}
  it should "prove <= & <=" in withMathematica { _ => check(metricAndLe)}
  it should "prove < & <" in withMathematica { _ => check(metricAndLt)}
  it should "prove <= | <=" in withMathematica { _ => check(metricOrLe)}
  it should "prove < | <" in withMathematica { _ => check(metricOrLt)}

  it should "prove const congruence" in withMathematica { _ => check(constCongruence)}
  it should "prove const formula congruence" in withMathematica { _ => check(constFormulaCongruence)}

  "Derived Axiom Tactics" should "tactically prove <-> reflexive" in {check(equivReflexiveAxiom)}
  it should "tactically prove !!" in {check(doubleNegationAxiom)}
  it should "tactically prove exists dual" in {check(existsDualAxiom)}
  it should "tactically prove all eliminate" taggedAs OptimisticTest ignore {check(allEliminateAxiom)}
  it should "tactically prove exists eliminate" in {check(existsEliminate)}
  it should "tactically prove all distribute" in {check(allDistributeAxiom)}
  it should "tactically prove box dual" in {check(boxAxiom)}
  it should "tactically prove <:=> assign" in {check(assigndAxiom)}
  it should "tactically prove [:=] equational" in withMathematica { _ => check(assignbEquationalAxiom)}
//  it should "tactically prove [:=] equational exists" in {check(assignbExistsAxiom, assignbEquationalT)}
  it should "tactically prove [:=] vacuous assign" in {check(vacuousAssignbAxiom)}
  it should "tactically prove <:=> vacuous assign" in {check(vacuousAssigndAxiom)}
  it should "tactically prove <':=> differential assign" in {check(assignDAxiom)}
  it should "tactically prove <++> choice" in {check(choicedAxiom)}
  it should "tactically prove <;> compose" in {check(composedAxiom)}
  it should "tactically prove <*> iterate" in {check(iteratedAxiom)}
  it should "tactically prove exists generalize" in {check(existsGeneralize)}
  it should "tactically prove = reflexive" in withMathematica { _ => check(equalReflex)}
  it should "tactically prove = commute" in withMathematica { _ => check(equalCommute)}
  it should "tactically prove <=" in withMathematica { _ => check(lessEqual)}
  it should "tactically prove ! !=" in withMathematica { _ => check(notNotEqual)}
  it should "tactically prove ! >=" in withMathematica { _ => check(notGreaterEqual)}
  it should "tactically prove >= flip" in withMathematica { _ => check(flipGreaterEqual)}
  it should "tactically prove > flip" in withMathematica { _ => check(flipGreater)}
  it should "tactically prove all substitute" in {check(allSubstitute)}
  it should "tactically prove vacuous exists" in {check(vacuousExistsAxiom)}
  it should "tactically prove V[:*] vacuous assign nondet" in {check(vacuousBoxAssignNondetAxiom)}
  it should "tactically prove V<:*> vacuous assign nondet" in {check(vacuousDiamondAssignNondetAxiom)}
  it should "tactically prove \\forall->\\exists" in {check(forallThenExistsAxiom)}
  //it should "tactically prove DI differential invariance" in {check(DIinvariance)}
  it should "tactically prove DI differential invariant" in {check(DIinvariant)}
  it should "tactically prove DG differential pre-ghost" in {check(DGpreghost)}
  it should "tactically prove DW differential weakening" in {check(DWeakening)}
  it should "tactically prove abs" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      check(absDef)
    }
  }
  it should "tactically prove min" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      check(minDef)
    }
  }
  it should "tactically prove max" in withMathematica { _ =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      check(maxDef)
    }
  }

  "Mathematica" should "derive compatibility axiom dgZeroEquilibrium" in withMathematica { _ =>
    import TactixLibrary._
    val dgZeroEquilibrium = AxiomInfo.ofCodeName("dgZeroEquilibrium")
    dgZeroEquilibrium.formula shouldBe "x=0 & n>0 -> [{x'=c*x^n}]x=0".asFormula

    TactixLibrary.proveBy(dgZeroEquilibrium.formula,
      implyR(1) & dG("y' = ( (-c*x^(n-1)) / 2)*y".asDifferentialProgram, Some("x*y^2=0&y>0".asFormula))(1) &
      TactixLibrary.boxAnd(1, 0::Nil) &
      DifferentialTactics.diffInd()(1, 0::0::Nil) &
      dG("z' = (c*x^(n-1)/4) * z".asDifferentialProgram, Some("y*z^2 = 1".asFormula))(1, 0::1::Nil) &
      dI()(1, 0::1::0::Nil) & QE
    ) shouldBe 'proved
  }

  "SimplifierV3" should "prove * identity neg" in withMathematica { _ => check{timesIdentityNeg}}
  it should "prove -0" in withMathematica { _ => check{minusZero}}
  it should "prove 0-" in withMathematica { _ => check{zeroMinus}}
  it should "prove >0 -> !=0"  in withMathematica { _ => check{gtzImpNez}}
  it should "prove <0 -> !=0"  in withMathematica { _ => check{ltzImpNez}}
  it should "prove !=0 -> 0/F" in withMathematica { _ => check{zeroDivNez}}
  it should "prove F^0" in withMathematica { _ => check{powZero}}
  it should "prove F^1"        in withMathematica { _ => check{powOne}}

  it should "prove < irrefl" in withMathematica { _ => check{lessNotRefl}}
  it should "prove > irrefl" in withMathematica { _ => check{greaterNotRefl}}
  it should "prove != irrefl" in withMathematica { _ => check{notEqualNotRefl}}
  it should "prove = refl"  in withMathematica { _ => check{equalRefl}}
  it should "prove <= refl"  in withMathematica { _ => check{lessEqualRefl}}
  it should "prove >= refl"  in withMathematica { _ => check{greaterEqualRefl}}

  it should "prove = sym"  in withMathematica { _ => check{equalSym}}
  it should "prove != sym"  in withMathematica { _ => check{equalSym}}
  it should "prove > antisym"  in withMathematica { _ => check{greaterNotSym}}
  it should "prove < antisym"  in withMathematica { _ => check{lessNotSym}}

  //@note must be last to populate the lemma database during build
  "The DerivedAxioms prepopulation procedure" should "not crash" taggedAs KeYmaeraXTestTags.CheckinTest in withMathematica { _ =>
    LemmaDBFactory.lemmaDB.deleteDatabase()
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      val writeEffect = true
      // use a new instance of the DerivedAxioms singleton object to store all axioms to the lemma database
      val c = DerivedAxioms.getClass.getDeclaredConstructor()
      c.setAccessible(true)
      c.newInstance().asInstanceOf[DerivedAxioms.type].prepopulateDerivedLemmaDatabase()

      val cache = new File(Configuration.path(Configuration.Keys.LEMMA_CACHE_PATH))
      // see [[FileLemmaDB.scala]] for path of actual lemma files
      val lemmaFiles = new File(cache.getAbsolutePath + File.separator + "lemmadb").listFiles(new FilenameFilter() {
        override def accept(dir: File, name: String): Boolean = name.endsWith(".alp")
      }).map(_.getName.stripSuffix(".alp"))

      val lemmas = getDerivedAxiomsMirrors.map({ case (valName, m) => valName -> m().asInstanceOf[Lemma] })
      // we allow lazy val forwarding, but all of them have to refer to the same lemma
      val forwards = lemmas.groupBy({ case (_, lemma) => lemma }).filter(_._2.length > 1)
      println("Lemma forwards:\n" + forwards.map(f => f._1.name.getOrElse("<anonymous>") + " <- " + f._2.map(_._1).mkString("[", ",", "]")).mkString("\n"))
      // the lemma database only stores the one lemma referred to from one or more lazy vals
      lemmaFiles.length shouldBe lemmaFiles.distinct.length
      // the lemma database stores all the distinct lemmas in DerivedAxioms
      forwards.keys.flatMap(_.name).toList.diff(lemmaFiles) shouldBe empty

      if (writeEffect) {
        val versionFile = new File(cache.getAbsolutePath + File.separator + "VERSION")
        if (!versionFile.exists()) {
          if (!versionFile.createNewFile()) throw new Exception(s"Could not create ${versionFile.getAbsolutePath}")
        }
        assert(versionFile.exists())
        val fw = new FileWriter(versionFile)
        fw.write(edu.cmu.cs.ls.keymaerax.core.VERSION)
        fw.close()
      }
    }
  }

  /** Returns the reflection mirrors to access the lazy vals in DerivedAxioms. */
  private def getDerivedAxiomsMirrors = {
    val lemmas = DerivedAxioms.getClass.getDeclaredFields.filter(f => classOf[Lemma].isAssignableFrom(f.getType))
    val fns = lemmas.map(_.getName)

    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    // access the singleton object
    val moduleMirror = mirror.reflectModule(ru.typeOf[DerivedAxioms.type].termSymbol.asModule)
    val im = mirror.reflect(moduleMirror.instance)

    //@note lazy vals have a "hidden" getter method that does the initialization
    val fields = fns.map(fn => fn -> ru.typeOf[DerivedAxioms.type].member(ru.TermName(fn)).asMethod.getter.asMethod)
    fields.map(f => f._2.toString -> im.reflectMethod(f._2))
  }
}
