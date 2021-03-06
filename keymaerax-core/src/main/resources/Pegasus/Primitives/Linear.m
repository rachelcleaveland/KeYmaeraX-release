(* ::Package:: *)

Needs["Primitives`",FileNameJoin[{Directory[],"Primitives","Primitives.m"}]]
Needs["FirstIntegrals`",FileNameJoin[{Directory[],"Primitives","FirstIntegrals.m"}]] 


BeginPackage[ "Linear`"]


LinearMethod::usage="LinearMethod[problem_List]"
Options[LinearMethod]={RationalsOnly->False, RationalPrecision->10, FirstIntegralDegree->2};
PlanarConstantMethod::usage="ConstantMethod[problem_List]"
LinearClass::usage="LinearClass[matrix]"


Begin["`Private`"]


LinearClass[M_?MatrixQ]:=Module[{},
Which[
AllTrue[Eigenvalues[M],Re[#]<0&], "Stable",
AllTrue[Eigenvalues[M],Re[#]>0&], "Unstable",
AllTrue[Eigenvalues[M],Re[#]==0&], "Centre",
True, "Other"]
]


EigenspacePolys[M_List,vars_List]:=Module[{},
Map[Rest[#].vars&,Select[Eigensystem[Transpose[M]]//Transpose,Element[First[#],Reals] &]]//Flatten
]


FirstIntegrals[M_List,vars_List,deg_Integer?NonNegative]:=Module[{},
(* Compute functionally independent first integrals up to given degree *)
FirstIntegralGen`FuncIndep[FirstIntegralGen`FindFirstIntegrals[deg, vars, M.vars],vars]
]


LyapunovIdentity[M_,vars_List]:=Module[{ID,NDM,LM},
ID=IdentityMatrix[Length[vars]];
NDM=-(ID);
LM=LyapunovSolve[Transpose[M],NDM];
Expand[vars.LM.vars]
]


RandomPosMatrix[range_?IntegerQ,size_]:=Module[{},
Table[Table[RandomInteger[{1,range}]/RandomInteger[{1,range}],{i,1,size}],{j,1,size}]
]


LyapunovRand[M_,vars_List]:=Module[{RM, NDM, LM},
RM=RandomPosMatrix[5,Length[vars]];
NDM=-(RM.Transpose[RM]);
LM=LyapunovSolve[Transpose[M],NDM];
Expand[vars.LM.vars]
]


LinearMethod[problem_List, opts:OptionsPattern[]]:=Module[{
pre, vf, vars, evoConst, post,
M,rats,FIDeg,ratPrecision,
initConnectedComponents,class
},
{pre,{vf,vars,evoConst},post}=problem;
M=Grad[vf, vars];

(* Process options *)
rats=TrueQ[OptionValue[RationalsOnly]];
FIDeg=If[TrueQ[OptionValue[FirstIntegralDegree]>0],OptionValue[FirstIntegralDegree],2];
ratPrecision=If[TrueQ[OptionValue[RationalPrecision]>0],OptionValue[RationalPrecision],10];

(* Compute the connected components of the initial set *)
initConnectedComponents=CylindricalDecomposition[pre,vars,"Components"];

class=LinearClass[M]/.{
"Centre" :> Block[{
  FIs=FirstIntegrals`FuncIndep[FirstIntegrals`FindFirstIntegrals[FIDeg, vars, vf],vars],
  maxFns,minFns,maxmin},
  Print["Linear Centre"];
  maxFns = Map[#[[1]]-Primitives`UpperRatCoeffs[MaxValue[#,vars]/.{Infinity -> 0,-Infinity -> 0},vars,ratPrecision] &, Tuples[{FIs, initConnectedComponents}] ];
  minFns = Map[#[[1]]-Primitives`LowerRatCoeffs[MinValue[#,vars]/.{Infinity -> 0,-Infinity -> 0},vars,ratPrecision] &, Tuples[{FIs, initConnectedComponents}] ];
  maxmin = Transpose[{maxFns, minFns}];
  Flatten[maxmin]
  (*Map[And[#[[1]]\[LessEqual]0,#[[2]]\[GreaterEqual]0]&,maxmin] /. List \[Rule] Or*)
],
"Stable":> Block[{LyapunovFn,Krasovskii,
  separatrices=EigenspacePolys[M, vars],
  maximise,minimise,maxFns,minFns,maxmin},
  Print["Linear Stable"];
  LyapunovFn=LyapunovIdentity[M,vars];
  Krasovskii=vf.vf; 
  maximise={LyapunovFn, Krasovskii};
  maxFns = Map[#[[1]]-Primitives`UpperRatCoeffs[MaxValue[#,vars]/.{Infinity -> 0,-Infinity -> 0},vars,ratPrecision] &, Tuples[{maximise, initConnectedComponents}] ];
  separatrices=If[rats,Map[Primitives`UpperRatCoeffs[#,vars,ratPrecision]&, separatrices], separatrices];
  Union[separatrices, maxFns]
],
"Unstable":> Block[{LyapunovFn,Krasovskii,
  separatrices=EigenspacePolys[M, vars],
  minimise,minFns},
  Print["Linear Unstable"];
  LyapunovFn=LyapunovIdentity[-M,vars];
  Krasovskii=(-vf).(-vf); 
  minimise={LyapunovFn, Krasovskii};
  minFns = Map[#[[1]]-Primitives`LowerRatCoeffs[MinValue[#,vars]/.{Infinity -> 0,-Infinity -> 0},vars,ratPrecision] &, Tuples[{minimise, initConnectedComponents}] ];
  separatrices=If[rats,Map[Primitives`UpperRatCoeffs[#,vars,ratPrecision]&, separatrices], separatrices];
  Union[separatrices, minFns]
],
"Other":> Block[{
  FIs=FirstIntegrals`FuncIndep[FirstIntegrals`FindFirstIntegrals[FIDeg, vars, vf],vars],
  (* Compute the linear forms of the invariant sub-spaces *)
  separatrices=EigenspacePolys[M, vars],
  ls,maxFns,minFns,maxmin},
  Print["Linear Other"];
  maxFns = Map[#[[1]]-Primitives`UpperRatCoeffs[MaxValue[#,vars]/.{Infinity -> 0,-Infinity -> 0},vars,ratPrecision] &, Tuples[{FIs, initConnectedComponents}] ];
  minFns = Map[#[[1]]-Primitives`LowerRatCoeffs[MinValue[#,vars]/.{Infinity -> 0,-Infinity -> 0},vars,ratPrecision] &, Tuples[{FIs, initConnectedComponents}] ];
  maxmin = Transpose[{maxFns, minFns}];
  separatrices=If[rats,Map[Primitives`UpperRatCoeffs[#,vars,ratPrecision]&, separatrices], separatrices];
  Union[maxmin,separatrices]//Flatten
]
};
class
]


Options[PlanarConstantMethod]={RationalsOnly->False, RationalPrecision->10, FirstIntegralDegree->1};
PlanarConstantMethod[Init_, Postcond_, System_List, opts:OptionsPattern[]]:=Module[{
rats,FIDeg,ratPrecision,upperRat,lowerRat,initConnectedComponents,FIs,maximise,minimise,
maxFns,minFns,partitioning,
f=System[[1]],
statevars=System[[2]]
},

(* Process options *)
rats=TrueQ[OptionValue[RationalsOnly]];
FIDeg=If[TrueQ[OptionValue[FirstIntegralDegree]>0],OptionValue[FirstIntegralDegree],1];
ratPrecision=If[TrueQ[OptionValue[RationalPrecision]>0],OptionValue[RationalPrecision],10];

(* Create rationalization function wrappers *)
upperRat[num_]:=If[TrueQ[rats && Element[num,Reals]], UpperRat[num, ratPrecision], num];
lowerRat[num_]:=If[TrueQ[rats && Element[num,Reals]], LowerRat[num, ratPrecision], num];

(* Compute the connected components of the initial set *)
initConnectedComponents=CylindricalDecomposition[Init,statevars,"Components"];

(* Compute first integral *)
FIs=FirstIntegralGen`FuncIndep[FirstIntegralGen`FindFirstIntegrals[FIDeg, statevars, f],statevars];

maximise=Union[FIs,{f.statevars}]//Flatten;
minimise=Union[FIs,{f.statevars}]//Flatten;

maxFns = Map[#[[1]]-upperRat[MaxValue[#,statevars]/.{Infinity -> 0,-Infinity -> 0}] &, Tuples[{maximise, initConnectedComponents}] ];
minFns = Map[#[[1]]-lowerRat[MinValue[#,statevars]/.{Infinity -> 0,-Infinity -> 0}] &, Tuples[{minimise, initConnectedComponents}] ];
Print[Union[ maxFns, minFns]];
partitioning=Union[ maxFns, minFns];
partitioning
]


End[]
EndPackage[]
