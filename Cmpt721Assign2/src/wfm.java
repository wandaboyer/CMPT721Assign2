import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;


public class wfm {
	public ArrayList<atom> T_Pi;
	public ArrayList<atom> F_Pi;
	public ArrayList<atom> A;
	
	public ArrayList<definiteClause> definiteClauseList;
	
	public wfm () {
		A = new ArrayList<atom>();
		T_Pi = new ArrayList<atom>();
		F_Pi = new ArrayList<atom>();
		definiteClauseList = new ArrayList<definiteClause>();
	}
		
	private void readProblem(Scanner sc) throws IncorrectInputException {
		String scNextLine;
		String[] tokens;		
		definiteClause dummyDC;
		System.out.println("Input Problem:");
		while (sc.hasNext()) {
			scNextLine = sc.nextLine();
			if (scNextLine.contains("#")){
				if (scNextLine.indexOf("#") == 0) {
					scNextLine = ""; // I AM CHOOSING THE BEHAVIOUR TO IGNORE EMPTY DEFINITE CLAUSES IN THE KB (otherwise, automatically unsat!)
				}
				else{
					scNextLine = scNextLine.substring(0, scNextLine.indexOf("#") - 1).trim();
				}
			}
			dummyDC = new definiteClause(scNextLine);
			System.out.println(dummyDC);
			this.definiteClauseList.add(dummyDC);
			if (!this.A.contains(dummyDC.head)) {
				this.A.add(dummyDC.head);
			}
			addToList(dummyDC.posAtoms, this.A);
			addToList(dummyDC.negAtoms, this.A);
		}
		System.out.println();
	}
	
	private void solve() {
		/*
		 * 	Loop until there are no changes to T or F:		
		 */
		ArrayList<atom> Tnew;
		ArrayList<atom> Fnew = new ArrayList<atom>();
		ArrayList<atom> Tposs;
		ArrayList<atom> union = new ArrayList<atom>();
		ArrayList<definiteClause> dummyDCList = new ArrayList<definiteClause>();
		definiteClause clauseToConsider;
		
		do {
			/*
			 * 1. 	(a) Run the bottom up procedure on those rules that don’t contain a
			 *	negated atom in the body.
			 *		(b) Call the set of newly-derived atoms T^{new} . (These atoms must be true.)
			 */
			for (int i = 0; i < this.definiteClauseList.size(); i++) {
				clauseToConsider = this.definiteClauseList.get(i);
				if (clauseToConsider.negAtoms.isEmpty()) {
					dummyDCList.add(clauseToConsider);
				}
			}
			
			System.out.println("dummyDCList before compile: "+dummyDCList);
			
			Tnew = bottomUp(dummyDCList);
			System.out.println("Tnew: "+Tnew);
			/*
			 * 		(c) Add these atoms to T.
			 */
			System.out.println("this.T_Pi old: "+this.T_Pi);
			addToList(Tnew, this.T_Pi);
			System.out.println("this.T_Pi new: "+this.T_Pi);
			/*
			 * 		(d) “Compile” the atoms in T^{new} into the set of rules:
			 *			For a \in T^{new} ,
			 *				– delete any rule with a in the head or \tilde a in the body,
			 *				– remove any occurrences of a from the remaining rules.
			 */
			for (int i = 0; i < dummyDCList.size(); i++) {
				if (Tnew.contains(dummyDCList.get(i).head)) {
					dummyDCList.remove(i);
				}
				else {
					dummyDCList.get(i).negAtoms.removeAll(Tnew);
				}
			}
			for (int i = 0; i < dummyDCList.size(); i++) {
				for (int j = 0; j < Tnew.size(); j++) {
					dummyDCList.get(i).posAtoms.remove(Tnew.get(j));
				}
			}
			
			System.out.println("dummyDCList after Compile: "+dummyDCList);
			
			/*
			 * is dummyDCList the listof rules that I'm supposed to be referring to?? or should I start over using the original this.definiteClauseList?
			 * Because how can i use the old rules
			 */
			if (!dummyDCList.isEmpty()) {
				System.out.println("dummyDCList nonempty");
				/*
				 * 2. 	(a) Run the bottom up procedure on the rule set, but ignoring all negated
				 *	atoms.
				 *		(b) Call this set T^{poss}. (These atoms might potentially become true later.)
				 */
				for (int i = 0; i < dummyDCList.size(); i++) {
					dummyDCList.get(i).negAtoms = new ArrayList<atom>();
				}
				System.out.println("dummyDCList after ignoring negative atoms: "+dummyDCList);
				Tposs = bottomUp(dummyDCList);
				
				/*
				 * 		(c) Let F^{new} be those atoms whose truth value is “unknown” and that don’t
				 *			appear in T^{poss}. (The atoms in F^{new} are those that cannot possibly be
				 *			true, and so are (now) known to be false.)
				 */
				union.addAll(Tnew);
				union.addAll(Tposs);
				Fnew = arrayListSetDiff(this.A, union);
				
				/*
				 * 		(d) Add the atoms in F^{new} to F.
				 */
				addToList(Fnew, this.F_Pi);
				
				/*
				 * 		(e) “Compile” the atoms in F^{new} into the set of rules:
				 *			For a \in F^{new},
				 *				– delete any rule with a in the head or a in the body,
				 *				– remove any occurrences of ~ a from the remaining rules.
				 */
				for (int i = 0; i < dummyDCList.size(); i++) {
					if (Fnew.contains(dummyDCList.get(i).head) || !arrayListSetDiff(Fnew, dummyDCList.get(i).posAtoms).isEmpty() || !arrayListSetDiff(Fnew, dummyDCList.get(i).negAtoms).isEmpty()) {
						dummyDCList.remove(i);
					}
					else {
						dummyDCList.get(i).negAtoms.removeAll(Fnew);
					}
				}
				for (int i = 0; i < dummyDCList.size(); i++) {
					for (int j = 0; j < Fnew.size(); j++) {
						dummyDCList.get(i).posAtoms.remove(Fnew.get(j));
					}
				}
			}
			printAtomLists(false);
		} while (!(arrayListSetDiff(Tnew, this.T_Pi).isEmpty() && arrayListSetDiff(Fnew, this.F_Pi).isEmpty()));
	}

	/*
	 * C := {};
	 * repeat
	 * 		choose all r \in A (the set of rules) such that
	 * 			r is a rule of the form 'h \Leftarrow b_1 \wedge \ldots \wedge b_m'
	 * 			b_i \in C for all i, and
	 * 			h \neg \in C;
	 * 				C := C \cup {h}
	 * until no more choices
	 */

	private ArrayList<atom> bottomUp(ArrayList<definiteClause> A) { // maybe create A' = A and then delete rules from A'  to make less runs
		ArrayList<atom> C = new ArrayList<atom>();
		ArrayList<atom> Cnew = new ArrayList<atom>();
		do {
			addToList(Cnew, C);
			Cnew = new ArrayList<atom>();
			for (int i = 0; i < A.size(); i++) {
				if ((A.get(i).posAtoms.isEmpty() || C.containsAll(A.get(i).posAtoms)) && !C.contains(A.get(i).head)) {
					Cnew.add(A.get(i).head);
				}
			}
		} while (!arrayListSetDiff(Cnew, C).isEmpty());
		return C;
	}
	
	private ArrayList<atom> arrayListSetDiff(ArrayList<atom> A, ArrayList<atom> B) {
		ArrayList<atom> setDiff = new ArrayList<atom>();
		setDiff.addAll(A);
		for (int i = 0; i < B.size(); i++) {
			setDiff.remove(B.get(i));
		}
		return setDiff;
	}
	
	private void addToList(ArrayList<atom> listOfAtomsToAdd, ArrayList<atom> existingAtomList) {
		atom dummyAtom = new atom(null);
		for (int i = 0; i < listOfAtomsToAdd.size(); i++) {
			dummyAtom = listOfAtomsToAdd.get(i);
			if (!existingAtomList.contains(dummyAtom)) {
				existingAtomList.add(dummyAtom);
			}
		}
	}
	
	private void printAtomLists(boolean printAToo) {
		if (printAToo){
			System.out.println("A: "+ this.A);
		}
		System.out.println("T_Pi: "+ this.T_Pi+ ", F_Pi: "+ this.F_Pi + "\n");
	}
	
	public static void main (String[] args){ 		
		wfm program = new wfm();
		try {
			Scanner sc = new Scanner(new File(args[0]));
			program.readProblem(sc);
			program.printAtomLists(true);
			program.solve();
			System.out.println ("All done!");
		} 
		catch (FileNotFoundException e) {
			System.out.println("File '"+args[0]+"' not found!");
		}
		catch (IncorrectInputException e) {
			System.out.println(e.getMessage());
		}
		catch (Exception e) {
			System.out.println("General Exception: ");
			e.printStackTrace();
		}
	}
}
