import java.util.*;
import java.io.*;

// Transforms the Tokenized Program into Native Assembly

public class Parser {
    // ArrayList of tokens representing the original program
    private ArrayList<Token> tokenList;
    private int tokenIndex;
    // Token Storing Current Function Being Created
    private Token funToken;
    // Labels for determining branching jumps
    private int labelCounter;
    // Variables grouped by scope
    private HashMap<String, Token> variables;
    // Lists of function parameters
    private HashMap<String, ArrayList<Token>> functionParameters;
    // Program stored line-by-line
    private ArrayList<String> programLines;
    private PrintWriter out;

    // Constructor Method
    public Parser(ArrayList<Token> stuff, String outputFileName) throws IOException {
        // Make the token list we will use for parsing by removing all "int" and "float" tokens
        tokenList = new ArrayList<Token>();
        for (int i = 0; i < stuff.size(); i++)
            if (stuff.get(i).getType() != Token.Types.INT && stuff.get(i).getType() != Token.Types.FLOAT)
                tokenList.add(stuff.get(i));
        tokenIndex = 0;
        labelCounter = 0;
        // Initializes the HashMap organizing variables by their scope
        for (Token t: tokenList) {
            if (t.getType() != Token.Types.ID) continue;
            // Puts variable into HashMap
            String varName = t.getName();
            if (varName.charAt(0) != 'f') {
                variables.put(varName, t);
            }
        }
        // Store parameters for each function for easy lookup in the future
        for (int i = 0; i < tokenList.size(); i++) {
            if (tokenList.get(i).getType() != Token.Types.FUN) continue;
            else i++;
            String funName = tokenList.get(i).getName(); i += 2;
            functionParameters.put(funName, new ArrayList<Token>());
            while (tokenList.get(i).getType() == Token.Types.ID) {
                functionParameters.get(funName).add(tokenList.get(i));
                i += 2;
            }
        }
        out = new PrintWriter(new FileWriter(outputFileName));
    }


    public void parse() throws Exception {
        // Useful defines (may be extended in the future)
        programLines.add("define MAX_STACK_SIZE 1024");
        // Initialize variables
        for (Token t: tokenList) {
            if (t.getType() != Token.Types.ID) continue;
            String varName = t.getName();
            if (varName.charAt(0) == 'f') {
                StringBuilder returnVar = new StringBuilder();
                if (t.getValue().isFloat()) returnVar.append("DATAF ");
                else returnVar.append("DATA32 ");
                returnVar.append(varName + "_ret");
                variables.put(varName + "_ret", t);
                programLines.add(returnVar.toString());
            }
            else if (t.getValue().isFloat()) {
                programLines.add("DATAF " + varName);
            }
            else {
                programLines.add("DATA32 " + varName);
            }
        }
        // Stores results that will be used for conditional branching
        programLines.add("DATA32 expressionResult");
        // Temporary storage for data type specific results between operations
        programLines.add("DATA32 raxInt");
        programLines.add("DATAF raxFloat");
        // Create stacks for rax items and stack pointers
        programLines.add("HANDLE intStack");
        programLines.add("DATA32 intStackPointer");
        programLines.add("HANDLE floatStack");
        programLines.add("DATA32 floatStackPointer");
        // TODO: Create variables for built-in function arguments
        generateBuiltInFunctions();
        // Handles Main by Putting it First
        for (int i = 0; i < tokenList.size(); i++){
            if (tokenList.get(i).getType() == Token.Types.FUN) {
                if (tokenList.get(i + 1).getName().equals("fmain")) {
                    // Initialize tokenIndex to intended position and funToken to the right token
                    tokenIndex = i; funToken = tokenList.get(i+1);
                    while (tokenList.get(tokenIndex).getType() != Token.Types.RPAREN) {
                        tokenIndex++;
                    }
                    tokenIndex++;
                    // Main Header
                    programLines.add("vmthread main {");
                    // Initialize stacks for rax items
                    programLines.add("ARRAY(CREATE32, MAX_STACK_SIZE, intStack)");
                    programLines.add("MOVE32_32(0, intStackPointer)");
                    programLines.add("ARRAY(CREATE32, MAX_STACK_SIZE, floatStack)");
                    programLines.add("MOVE32_32(0, floatStackPointer)");
                    processStatement();
                    // Closing Brace
                    programLines.add("}");
                    // Removes all tokens in the main from the list of tokens to be processed
                    int startOfMain = i, endOfMain = tokenIndex-1;
                    ArrayList<Token> remaining = new ArrayList<Token>();
                    for (int j = 0; j < tokenList.size(); j++) {
                        if (j < startOfMain || j > endOfMain) {
                            remaining.add(tokenList.get(j));
                        }
                    }
                    // Resets the tokenIndex
                    tokenList = remaining; tokenIndex = 0;
                    break;
                }
            }
        }
        // Do the recursive descent parsing for everything else remaining
        repeatProcessing();
        // Print out every line of the program
        for (int i = 0; i < programLines.size(); i++) {
            out.println(programLines.get(i));
        }
        out.close();
    }

    // Driver function for recursive descent parsing
    private void repeatProcessing() throws Exception {
        while(processStatement());
    }

    // Token-by-token processing
    private boolean processStatement() throws Exception {
        switch (tokenList.get(tokenIndex).getType()) {
            // Case of declaring a function
            case FUN: {
                tokenIndex++;
                funToken = tokenList.get(tokenIndex);
                while (tokenList.get(tokenIndex).getType() != Token.Types.RPAREN) {
                    tokenIndex++;
                }
                tokenIndex++;
                // Function Header
                programLines.add("subcall " + funToken.getName() + " {");
                processStatement();
                // Closing Brace
                programLines.add("}");
                return true;
            }
            // Case of returning in a function
            case RET : {
                tokenIndex++;
                processExpression();
                // Store in right return value
                if (funToken.getDataType() == Token.DataTypes.INT) {
                    programLines.add("MOVE32_32(raxInt, " + funToken.getName() + "_ret)");
                }
                else if (funToken.getDataType() == Token.DataTypes.FLOAT) {
                    programLines.add("MOVEF_F(raxFloat, " + funToken.getName() + "_ret)");
                }
                return true;
            }
            // Dealing with a variable or function call
            case ID: {
                Token curr = tokenList.get(tokenIndex); tokenIndex++;
                // Setting a variable
                if (tokenList.get(tokenIndex).getType() == Token.Types.EQ) {
                    processExpression();
                    if (curr.getDataType() == Token.DataTypes.INT) {
                        programLines.add("MOVE32_32(raxInt, " + curr.getName() + ")");
                    }
                    else if (curr.getDataType() == Token.DataTypes.FLOAT) {
                        programLines.add("MOVEF_F(raxFloat, " + curr.getName() + ")");
                    }
                    while(tokenList.get(tokenIndex).getType() == Token.Types.SEMI) tokenIndex++;
                }
                // Standalone function call
                else if (functionParameters.containsKey(curr.getName())) {
                    generateFunctionCall(curr);
                    return true;
                }
                // TODO: Stack or Queue Operations
                else {

                }
                return true;
            }
            case LBRACE: {
                tokenIndex++;
                repeatProcessing();
                if (tokenList.get(tokenIndex).getType() != Token.Types.RBRACE){
                    throw new Exception("Error at Token " + tokenIndex + "! Missing Matching Right Brace!");
                }
                tokenIndex++;
                return true;
            }
            // IF case also handles ELSE clause (if it exists)
            case IF : {
                tokenIndex++;
                int ifElse = labelCounter++;
                // Evaluates the IF condition
                processExpression();
                programLines.add("JR_EQ32(expressionResult, 0, LABEL_" + ifElse + ")");
                // Move through stuff before the potential ELSE statement
                processStatement();
                while(tokenList.get(tokenIndex).getType() == Token.Types.SEMI) tokenIndex++;
                // Casework based on if ELSE statement exists
                if (tokenList.get(tokenIndex).getType() == Token.Types.ELSE) {
                    tokenIndex++;
                    int ifEnd = labelCounter++;
                    programLines.add("JR(LABEL_" + ifEnd + ")");
                    programLines.add("LABEL_" + ifElse + ":");
                    processStatement();
                    programLines.add("LABEL_" + ifEnd + ":");
                }
                else {
                    programLines.add("LABEL_" + ifElse + ":");
                }
                return true;
            }
            case WHILE: {
                tokenIndex++;
                // Sets up top of the while loop and conditional
                int whileStart = labelCounter++;
                programLines.add("LABEL_" + whileStart + ":");
                processExpression();
                // Sets up jump to end of the while loop
                int whileEnd = labelCounter++;
                programLines.add("JR_EQ32(expressionResult, 0, LABEL_" + whileEnd + ")");
                processStatement();
                // Goes back to the start to check conditional
                programLines.add("JR(LABEL_" + whileStart + ")");
                programLines.add("LABEL_" + whileEnd + ":");
                return true;
            }
            /*
            // TODO
            case FOR: {

            }
            */
            // TODO: Add assembly to LDRIVE, and format other motor operations similarly to the below
            case LDRIVE: {
                tokenIndex++;
                // Store expression results in LDRIVE_port, LDRIVE_power, LDRIVE_rotations, LDRIVE_breakAtEnd
                // Gotta put these at the top of the code as well
                // Then run the actual assembly commands using those variables
                programLines.add("");
                return true;
            }
            case SEMI: {
                tokenIndex++;
                return true;
            }
            default: {
                return false;
            }
        }
    }

    // Generates assembly code for making a function call
    private void generateFunctionCall(Token curr) {
        tokenIndex++;
        int argumentNumber = 0;
        while (tokenList.get(tokenIndex).getType() != Token.Types.RPAREN) {
            processExpression();
            Token arg = functionParameters.get(curr.getName()).get(argumentNumber);
            if (arg.getDataType() == Token.DataTypes.INT) {
                programLines.add("MOVE32_32(raxInt, " + arg.getName() + ")");
            }
            else if (arg.getDataType() == Token.DataTypes.FLOAT) {
                programLines.add("MOVEF_F(raxFloat, " + arg.getName() + ")");
            }
            argumentNumber++;
        }
        tokenIndex++;
        programLines.add("CALL(" + curr.getName() + ")");
    }

    // TODO: See p4 code
    private void processExpression() {
        fourthStage();
    }

    // handles "==", "!=", ">=", ">", "<=", "<"
    private void fourthStage() {
        thirdStage();
        while (isFourthStageOperator()) {
            tokenIndex++;
        }
    }

    private boolean isFourthStageOperator() {
        Token.Types t = tokenList.get(tokenIndex).getType();
        return t == Token.Types.EQEQ ||
                t == Token.Types.NEQ ||
                t == Token.Types.GEQ ||
                t == Token.Types.GT ||
                t == Token.Types.LEQ ||
                t == Token.Types.LT;
    }

    private void thirdStage() {

    }

    private void secondStage() {

    }

    private void firstStage() {

    }

    // TODO: Add function parameters in the form of tokens just like with normal functions the user makes
    private void generateBuiltInFunctions() {
        // EX: LDRIVE
        // Store expression results in LDRIVE_port, LDRIVE_power, LDRIVE_rotations, LDRIVE_breakAtEnd
        // Then run the actual assembly commands using those variables
    }
}
