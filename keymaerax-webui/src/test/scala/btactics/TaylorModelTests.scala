package edu.cmu.cs.ls.keymaerax.btactics

import edu.cmu.cs.ls.keymaerax.Configuration
import edu.cmu.cs.ls.keymaerax.bellerophon._
import org.scalatest.LoneElement._
import edu.cmu.cs.ls.keymaerax.btactics.TaylorModelTactics._
import edu.cmu.cs.ls.keymaerax.parser.KeYmaeraXPrettierPrinter
import edu.cmu.cs.ls.keymaerax.parser.StringConverter._
import edu.cmu.cs.ls.keymaerax.core._
import TactixLibrary._
import TacticFactory._
import Augmentors._
import edu.cmu.cs.ls.keymaerax.pt.ProvableSig
import edu.cmu.cs.ls.keymaerax.tools.BigDecimalQETool

import scala.collection.immutable._

class TaylorModelTests extends TacticTestBase {

  "coarsenTimesBounds" should "work" in withMathematica { _ =>
    proveBy("0 <= t, t <= h(), t*f() <= xRem, xRem <= t*g(), p() ==> q()".asSequent, coarsenTimesBounds("t".asTerm)).
      subgoals.loneElement shouldBe ("0<=t, t<=h(), min((0,h()*f()))<=xRem, xRem<=max((0,h()*g())), p() ==>  q()".asSequent)
  }

  "TaylorModel" should "prove the lemma in order 2" in withMathematica { _ =>
    val ode = "{x' = 1 + y, y' = -x^2, t'=1}".asDifferentialProgram
    val tm = TaylorModel(ode, 2).lemma
    tm shouldBe 'proved
    //    println(new KeYmaeraXPrettierPrinter(100).stringify(tm.conclusion))
    tm.conclusion shouldBe
      """
        |==>
        |  (
        |    (t = t0() & x = a00() * r0() + a01() * r1() + aC0() & y = a10() * r0() + a11() * r1() + aC1()) &
        |    (rL0() <= r0() & r0() <= rU0()) & rL1() <= r1() & r1() <= rU1()
        |  ) &
        |  \forall s
        |    \forall Rem0
        |      \forall Rem1
        |        (
        |          (0 <= s & s <= h()) &
        |          (min((0,h() * iL0())) <= Rem0 & Rem0 <= max((0,h() * iU0()))) &
        |          min((0,h() * iL1())) <= Rem1 & Rem1 <= max((0,h() * iU1())) ->
        |          (
        |            iL0() <
        |            Rem1 +
        |            s *
        |            (
        |              r1() * (-2 * a01() * aC0()) + r0() * (-2 * a00() * aC0()) +
        |              s * (-aC0() + (-aC0()) * aC1())
        |            ) &
        |            Rem1 +
        |            s *
        |            (
        |              r1() * (-2 * a01() * aC0()) + r0() * (-2 * a00() * aC0()) +
        |              s * (-aC0() + (-aC0()) * aC1())
        |            ) <
        |            iU0()
        |          ) &
        |          iL1() <
        |          Rem0 * (-2 * aC0()) + r1() * (Rem0 * (-2 * a01())) +
        |          r0() * (Rem0 * (-2 * a00()) + r1() * (-2 * a00() * a01())) +
        |          -Rem0^2 +
        |          r1()^2 * (-a01()^2) +
        |          r0()^2 * (-a00()^2) +
        |          s *
        |          (
        |            Rem0 * (-2 + -2 * aC1()) +
        |            r1() * (-2 * a01() + -2 * a01() * aC1() + -2 * a11() * aC0() + Rem0 * (-2 * a11())) +
        |            r0() *
        |            (
        |              -2 * a00() + -2 * a00() * aC1() + -2 * a10() * aC0() + Rem0 * (-2 * a10()) +
        |              r1() * (-2 * a00() * a11() + -2 * a01() * a10())
        |            ) +
        |            r1()^2 * (-2 * a01() * a11()) +
        |            r0()^2 * (-2 * a00() * a10()) +
        |            s *
        |            (
        |              -1 + -2 * aC1() + -aC1()^2 + aC0()^3 + Rem0 * aC0()^2 +
        |              r1() * (-2 * a11() + -2 * a11() * aC1() + a01() * aC0()^2) +
        |              r0() *
        |              (-2 * a10() + -2 * a10() * aC1() + a00() * aC0()^2 + r1() * (-2 * a10() * a11())) +
        |              r1()^2 * (-a11()^2) +
        |              r0()^2 * (-a10()^2) +
        |              s *
        |              (
        |                aC0()^2 + aC0()^2 * aC1() + r1() * (a11() * aC0()^2) + r0() * (a10() * aC0()^2) +
        |                s * (-1 / 4 * aC0()^4)
        |              )
        |            )
        |          ) &
        |          Rem0 * (-2 * aC0()) + r1() * (Rem0 * (-2 * a01())) +
        |          r0() * (Rem0 * (-2 * a00()) + r1() * (-2 * a00() * a01())) +
        |          -Rem0^2 +
        |          r1()^2 * (-a01()^2) +
        |          r0()^2 * (-a00()^2) +
        |          s *
        |          (
        |            Rem0 * (-2 + -2 * aC1()) +
        |            r1() * (-2 * a01() + -2 * a01() * aC1() + -2 * a11() * aC0() + Rem0 * (-2 * a11())) +
        |            r0() *
        |            (
        |              -2 * a00() + -2 * a00() * aC1() + -2 * a10() * aC0() + Rem0 * (-2 * a10()) +
        |              r1() * (-2 * a00() * a11() + -2 * a01() * a10())
        |            ) +
        |            r1()^2 * (-2 * a01() * a11()) +
        |            r0()^2 * (-2 * a00() * a10()) +
        |            s *
        |            (
        |              -1 + -2 * aC1() + -aC1()^2 + aC0()^3 + Rem0 * aC0()^2 +
        |              r1() * (-2 * a11() + -2 * a11() * aC1() + a01() * aC0()^2) +
        |              r0() *
        |              (-2 * a10() + -2 * a10() * aC1() + a00() * aC0()^2 + r1() * (-2 * a10() * a11())) +
        |              r1()^2 * (-a11()^2) +
        |              r0()^2 * (-a10()^2) +
        |              s *
        |              (
        |                aC0()^2 + aC0()^2 * aC1() + r1() * (a11() * aC0()^2) + r0() * (a10() * aC0()^2) +
        |                s * (-1 / 4 * aC0()^4)
        |              )
        |            )
        |          ) <
        |          iU1()
        |        ) ->
        |  [{x'=1 + y, y'=-x^2, t'=1 & t0() <= t & t <= t0() + h()}]
        |    \exists s
        |      \exists Rem0
        |        \exists Rem1
        |          (
        |            t = t0() + s &
        |            (
        |              x =
        |              aC0() + s + a01() * r1() + a00() * r0() + s * aC1() + s * a11() * r1() +
        |              s * a10() * r0() +
        |              -1 / 2 * s^2 * aC0()^2 +
        |              Rem0 &
        |              s * iL0() <= Rem0 & Rem0 <= s * iU0()
        |            ) &
        |            y =
        |            aC1() + a11() * r1() + a10() * r0() + (-s) * aC0()^2 + (-s^2) * aC0() +
        |            -2 * s * a01() * aC0() * r1() +
        |            -2 * s * a00() * aC0() * r0() +
        |            (-s^2) * aC0() * aC1() +
        |            Rem1 &
        |            s * iL1() <= Rem1 & Rem1 <= s * iU1()
        |          )
      """.stripMargin.asSequent
  }

  it should "work for exp" in withMathematica { _ =>
    val ode = "{x' = x, t' = 1}".asDifferentialProgram
    val tm = TaylorModel(ode, 4).lemma
    tm shouldBe 'proved
    // println(new KeYmaeraXPrettierPrinter(100).stringify(tm.conclusion))
    tm.conclusion shouldBe
      """
        |==>
        |((t = t0() & x = a00() * r0() + aC0()) & rL0() <= r0() & r0() <= rU0()) &
        |  \forall s
        |    \forall Rem0
        |      (
        |        (0 <= s & s <= h()) & min((0,h() * iL0())) <= Rem0 & Rem0 <= max((0,h() * iU0())) ->
        |        iL0() < Rem0 + s * (s * (s * (r0() * (1 / 6 * a00()) + s * (1 / 24 * aC0())))) &
        |        Rem0 + s * (s * (s * (r0() * (1 / 6 * a00()) + s * (1 / 24 * aC0())))) < iU0()
        |      ) ->
        |  [{x'=x, t'=1 & t0() <= t & t <= t0() + h()}]
        |    \exists s
        |      \exists Rem0
        |        (
        |          t = t0() + s &
        |          x =
        |          aC0() + a00() * r0() + s * aC0() + s * a00() * r0() + 1 / 2 * s^2 * aC0() +
        |          1 / 2 * s^2 * a00() * r0() +
        |          1 / 6 * s^3 * aC0() +
        |          1 / 6 * s^3 * a00() * r0() +
        |          1 / 24 * s^4 * aC0() +
        |          Rem0 &
        |          s * iL0() <= Rem0 & Rem0 <= s * iU0()
        |        )
      """.stripMargin.asSequent
  }

  val vdp = "{x' = y, y' = (1 - x^2)*y - x,t'=1}".asDifferentialProgram

  it should "prove a lemma about van der Pol" in withMathematica { _ =>
    val tm = TaylorModel(vdp, 1).lemma
    tm shouldBe 'proved
  }

  it should "prove a lemma about Lotka-Volterra" in withMathematica { _ =>
    val ode = "{x' = 1.5*x - x*y, y'= -3*y + x*y, t' = 1}".asDifferentialProgram
    val tm = TaylorModel(ode, 2).lemma
    tm shouldBe 'proved
  }

  it should "prove a lemma about Lorenz" in withMathematica { _ =>
    val ode = "{x' = 10 * (y - x), y' = -y*z + 8/3*x - y, z' = x*y - 8/3*z, t' = 1}".asDifferentialProgram
    val tm = TaylorModel(ode, 1).lemma
    tm shouldBe 'proved
  }

  "cutTM" should "cut a Taylor model for exp, sin, cos" in withMathematica { qeTool =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      val ode = "{x' = x, y' = z, z' = y, t' = 1}".asDifferentialProgram
      val tm = TaylorModel(ode, 6)
      val box = Box(ODESystem(ode, "0 <= t & t <= 0 + 0.5".asFormula), "P(x, y, z)".asFormula)
      val assms = ("(t = 0 &" +
        "x = 0.01*r0() + 0*r1() + 0*r2() + 1 &" +
        "y = 0*r0() + 0.01*r1() + 0*r2() + 0.5 &" +
        "z = 0*r0() + 0*r1() + 0.01*r2() + 0) & " +
        "(-1 <= r0() & r0() <= 1) & (-1 <= r1() & r1() <= 1) & (-1 <= r2() & r2() <= 1)").asFormula
      val seq = Sequent(IndexedSeq(assms), IndexedSeq(box))
      val res1 = IntervalArithmeticV2Tests.timing("BigDecimalQETool")(() => proveBy(seq, tm.cutTM(10, AntePosition(1), BigDecimalQETool)(1)))
      val res2 = IntervalArithmeticV2Tests.timing("Mathematica     ")(() => proveBy(seq, tm.cutTM(10, AntePosition(1), qeTool)(1)))
      res1 shouldEqual res2
      val res = proveBy(res1, SimplifierV3.simpTac()(1, 0 :: 1 :: Nil))
      // println(new KeYmaeraXPrettierPrinter(80).stringify(res.subgoals.loneElement))
      res.subgoals.loneElement.ante.loneElement shouldBe assms
      res.subgoals.loneElement.succ.loneElement shouldBe
        """[
          |    {
          |      x'=x,
          |      y'=z,
          |      z'=y,
          |      t'=1 &
          |      (0 <= t & t <= 0.5) &
          |      \exists s
          |        \exists Rem0
          |          \exists Rem1
          |            \exists Rem2
          |              (
          |                t = s &
          |                (
          |                  x =
          |                  1 + 0.01 * r0() + s + s * 0.01 * r0() + 1 / 2 * s^2 +
          |                  1 / 2 * s^2 * 0.01 * r0() +
          |                  1 / 6 * s^3 +
          |                  1 / 6 * s^3 * 0.01 * r0() +
          |                  1 / 24 * s^4 +
          |                  1 / 24 * s^4 * 0.01 * r0() +
          |                  1 / 120 * s^5 +
          |                  1 / 120 * s^5 * 0.01 * r0() +
          |                  1 / 720 * s^6 +
          |                  Rem0 &
          |                  s * (-5400607643 * 10^-15) <= Rem0 &
          |                  Rem0 <= s * (4880338545 * 10^-14)
          |                ) &
          |                (
          |                  y =
          |                  0.5 + 0.01 * r1() + s * 0.01 * r2() + 1 / 2 * s^2 * 0.5 +
          |                  1 / 2 * s^2 * 0.01 * r1() +
          |                  1 / 6 * s^3 * 0.01 * r2() +
          |                  1 / 24 * s^4 * 0.5 +
          |                  1 / 24 * s^4 * 0.01 * r1() +
          |                  1 / 120 * s^5 * 0.01 * r2() +
          |                  1 / 720 * s^6 * 0.5 +
          |                  Rem1 &
          |                  s * (-5364438662 * 10^-15) <= Rem1 &
          |                  Rem1 <= s * (1259823498 * 10^-14)
          |                ) &
          |                z =
          |                0.01 * r2() + s * 0.5 + s * 0.01 * r1() +
          |                1 / 2 * s^2 * 0.01 * r2() +
          |                1 / 6 * s^3 * 0.5 +
          |                1 / 6 * s^3 * 0.01 * r1() +
          |                1 / 24 * s^4 * 0.01 * r2() +
          |                1 / 120 * s^5 * 0.5 +
          |                1 / 120 * s^5 * 0.01 * r1() +
          |                Rem2 &
          |                s * (-5355396416 * 10^-15) <= Rem2 &
          |                Rem2 <= s * (1982298904 * 10^-14)
          |              )
          |    }
          |  ]
          |    P((x,(y,z)))""".stripMargin.asFormula
    }
  }

  it should "cut a Taylor model for a nonlinear ODE" in withMathematica { qeTool =>
    withTemporaryConfig(Map(Configuration.Keys.QE_ALLOW_INTERPRETED_FNS -> "true")) {
      val ode = "{x' = 1 + y, y' = -x^2, t'=1}".asDifferentialProgram
      val tm = TaylorModel(ode, 2)
      val box = Box(ODESystem(ode, "0 <= t & t <= 0 + 0.1".asFormula), "P(x, y, z)".asFormula)
      val assms = ("(t = 0 &" +
        "x = 0.01*r0() + 0*r1() + 1 &" +
        "y = 0*r0() + 0.01*r1() + 0.5) &" +
        "(-1 <= r0() & r0() <= 1) & (-1 <= r1() & r1() <= 1)").asFormula
      val seq = Sequent(IndexedSeq(assms), IndexedSeq(box))
      val res1 = IntervalArithmeticV2Tests.timing("BigDecimalQETool")(() => proveBy(seq, tm.cutTM(10, AntePosition(1), BigDecimalQETool)(1)))
      val res2 = IntervalArithmeticV2Tests.timing("Mathematica     ")(() => proveBy(seq, tm.cutTM(10, AntePosition(1), qeTool)(1)))
      res1 shouldEqual res2
      val res = res1
      // println(new KeYmaeraXPrettierPrinter(80).stringify(res.subgoals.loneElement))
      res.subgoals.loneElement.ante.loneElement shouldBe assms
      res.subgoals.loneElement.succ.loneElement shouldBe
        """[
          |    {
          |      x'=1 + y,
          |      y'=-x^2,
          |      t'=1 &
          |      (0 <= t & t <= 0 + 0.1) &
          |      \exists s
          |        \exists Rem0
          |          \exists Rem1
          |            (
          |              t = 0 + s &
          |              (
          |                x =
          |                1 + s + 0 * r1() + 0.01 * r0() + s * 0.5 + s * 0.01 * r1() +
          |                s * 0 * r0() +
          |                -1 / 2 * s^2 * 1^2 +
          |                Rem0 &
          |                s * (-1887591619 * 10^-11) <= Rem0 &
          |                Rem0 <= s * (2940096162 * 10^-12)
          |              ) &
          |              y =
          |              0.5 + 0.01 * r1() + 0 * r0() + (-s) * 1^2 + (-s^2) * 1 +
          |              -2 * s * 0 * 1 * r1() +
          |              -2 * s * 0.01 * 1 * r0() +
          |              (-s^2) * 1 * 0.5 +
          |              Rem1 &
          |              s * (-1875812641 * 10^-11) <= Rem1 &
          |              Rem1 <= s * (9399926235 * 10^-12)
          |            )
          |    }
          |  ]
          |    P((x,(y,z)))
        """.stripMargin.asFormula
    }
  }
}