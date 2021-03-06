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
    // Stacks Mapped to Type
    private HashMap<String, Token.DataTypes> stackTypes;
    // Lists of function parameters
    private HashMap<String, ArrayList<Token>> functionParameters;
    // Set of variables that are function parameters
    private HashSet<String> parameterVars;
    // Program stored line-by-line
    private ArrayList<String> programLines;
    private PrintWriter out;

    // Constructor Method
    public Parser(ArrayList<Token> stuff, String outputFileName) throws IOException {
        // Make the token list we will use for parsing by removing all "int" and "float" tokens
        tokenList = new ArrayList<Token>();
        for (int i = 0; i < stuff.size(); i++) {
            Token.Types t = stuff.get(i).getType();
            if (t != Token.Types.INT && t != Token.Types.FLOAT) {
                tokenList.add(stuff.get(i));
            }
        }
        tokenIndex = 0;
        labelCounter = 0;
        // Store parameters for each function for easy lookup in the future
        functionParameters = new HashMap<String, ArrayList<Token>>();
        parameterVars = new HashSet<String>();
        for (int i = 0; i < tokenList.size(); i++) {
            if (tokenList.get(i).getType() != Token.Types.FUN) continue;
            else i++;
            String funName = tokenList.get(i).getName(); i += 2;
            functionParameters.put(funName, new ArrayList<Token>());
            while (tokenList.get(i).getType() != Token.Types.RPAREN) {
                if (tokenList.get(i).getType() == Token.Types.ID) {
                    functionParameters.get(funName).add(tokenList.get(i));
                    parameterVars.add(tokenList.get(i).getName());
                }
                i++;
            }
        }
        programLines = new ArrayList<String>();
        stackTypes = new HashMap<String, Token.DataTypes>();
        out = new PrintWriter(new FileWriter(outputFileName));
    }


    public void parse() throws Exception {
        // Useful defines (may be extended in the future)
        programLines.add("define MAX_STACK_SIZE 1024");
        // Initialize variables
        // TODO: Fix multiple initialization
        HashSet<String> alreadyInitialized = new HashSet<String>();
        for (Token t: tokenList) {
            if (t.getType() != Token.Types.ID) {
                continue;
            }
            String varName = t.getName();
            if (alreadyInitialized.contains(varName)) {
                continue;
            }
            if (varName.charAt(0) == 'f') {
                StringBuilder returnVar = new StringBuilder();
                if (t.getDataType() == Token.DataTypes.FLOAT) {
                    returnVar.append("DATAF ");
                }
                else if (t.getDataType() == Token.DataTypes.INT) {
                    returnVar.append("DATA32 ");
                }
                returnVar.append(varName + "_ret");
                programLines.add(returnVar.toString());
            }
            else if (t.getDataType() == Token.DataTypes.FLOAT) {
                String line = "DATAF " + varName;
                if (parameterVars.contains(varName)) {
                    line +=  "_param";
                }
                programLines.add(line);
            }
            else if (t.getDataType() == Token.DataTypes.INT) {
                String line = "DATA32 " + varName;
                if (parameterVars.contains(varName)) {
                    line +=  "_param";
                }
                programLines.add(line);
            }
            alreadyInitialized.add(varName);
        }
        // Temporary storage for data type specific results between operations
        programLines.add("DATA32 raxInt");
        programLines.add("DATAF raxFloat");
        programLines.add("DATA8 raxUse");
        programLines.add("DATA32 rdiInt");
        programLines.add("DATAF rdiFloat");
        // Flag for comparison results
        programLines.add("DATA8 compareFlag");
        // Create stacks for rax items and stack index
        programLines.add("HANDLE intStack"); // 0 in useStack
        programLines.add("HANDLE floatStack"); // 1 in useStack
        programLines.add("HANDLE useStack");
        programLines.add("DATA32 stackPointer");
        // TODO: Create variables for built-in function arguments
        generateBuiltInFunctionParameters();
        // Handles Main by Putting it First
        for (int i = 0; i < tokenList.size(); i++){
            if (tokenList.get(i).getType() == Token.Types.FUN && tokenList.get(i + 1).getName().equals("fmain")) {
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
                programLines.add("ARRAY(CREATEF, MAX_STACK_SIZE, floatStack)");
                programLines.add("ARRAY(CREATE8, MAX_STACK_SIZE, useStack)");
                programLines.add("MOVE32_32(0, stackPointer)");
                // Initialize rax stuffs
                programLines.add("MOVE32_32(0, raxInt)");
                programLines.add("MOVEF_F(0F, raxFloat)");
                programLines.add("MOVE8_8(0, raxUse)");
                // Does recursive descent parsing on the main
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
        // Do the recursive descent parsing for everything else remaining
        repeatProcessing();
        // TODO: Add our built in functions to the end
        addBuiltInFunctionAssembly();
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
        System.out.println(tokenIndex + " " + tokenList.get(tokenIndex).toString());
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
                // Specify parameters
                for (Token arg : functionParameters.get(funToken.getName())) {
                    if (arg.getDataType() == Token.DataTypes.INT) {
                        programLines.add("IN_32 " + arg.getName());
                    }
                    else if (arg.getDataType() == Token.DataTypes.FLOAT) {
                        programLines.add("IN_F " + arg.getName());
                    }
                    else if (arg.getDataType() == Token.DataTypes.INTSTACK || arg.getDataType() == Token.DataTypes.FLOATSTACK) {
                        programLines.add("IN_H " + arg.getName());
                    }
                }
                processStatement();
                // Closing Brace
                programLines.add("}");
                return true;
            }
            // Case of returning in a function
            case RET: {
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
                System.out.println("ID " + tokenIndex);
                Token curr = tokenList.get(tokenIndex); tokenIndex++;
                // Setting a variable
                if (tokenList.get(tokenIndex).getType() == Token.Types.EQ) {
                    tokenIndex++;
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
                return true;
            }
            case LBRACE: {
                System.out.println("LBRACE " + tokenIndex);
                tokenIndex++;
                repeatProcessing();
                if (tokenList.get(tokenIndex).getType() != Token.Types.RBRACE){
                    System.out.println(tokenList.get(tokenIndex));
                    throw new Exception("Error at Token " + tokenIndex + "! Missing Matching Right Brace!");
                }
                System.out.println("RBRACE " + tokenIndex);
                tokenIndex++;
                return true;
            }
            // IF case also handles ELSE clause (if it exists)
            case IF: {
                tokenIndex++;
                int ifElse = labelCounter++;
                // Evaluates the IF condition
                processExpression();
                programLines.add("JR_EQ32(raxInt, 0, LABEL_" + ifElse + ")");
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
                programLines.add("JR_EQ32(raxInt, 0, LABEL_" + whileEnd + ")");
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
            // Stack initialization
            case STACK: {
                Token.DataTypes dt = tokenList.get(tokenIndex).getDataType(); tokenIndex++;
                String stackName = tokenList.get(tokenIndex).getName(); tokenIndex++;
                programLines.add("HANDLE " + stackName);
                programLines.add("DATA32 " + stackName + "_Pointer");
                if (dt == Token.DataTypes.FLOATSTACK) {
                    programLines.add("ARRAY(CREATEF, MAX_STACK_SIZE, " + stackName + ")");
                }
                else if (dt == Token.DataTypes.INTSTACK) {
                    programLines.add("ARRAY(CREATE32, MAX_STACK_SIZE, " + stackName + ")");
                }
                programLines.add("MOVE32_32(0, " + stackName + "_Pointer)");
                // Adds stack to lookup table
                stackTypes.put(stackName, dt);
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
            if (tokenList.get(tokenIndex).getType() == Token.Types.COMMA) {
                tokenIndex++;
                continue;
            }
            processExpression();
            Token arg = functionParameters.get(curr.getName()).get(argumentNumber);
            if (arg.getDataType() == Token.DataTypes.INT) {
                programLines.add("MOVE32_32(raxInt, " + arg.getName() + "_param)");
            }
            else if (arg.getDataType() == Token.DataTypes.FLOAT) {
                programLines.add("MOVEF_F(raxFloat, " + arg.getName() + "_param)");
            }
            argumentNumber++;
        }
        tokenIndex++;
        StringBuilder call = new StringBuilder("CALL(" + curr.getName());
        // Create call
        for (int i = 0; i < functionParameters.get(curr.getName()).size(); i++) {
            Token arg = functionParameters.get(curr.getName()).get(i);
            call.append(", " + arg.getName() + "_param");
        }
        call.append(")");
        programLines.add(call.toString());
    }

    private void stackMemoryPush() {
        // Write to stacks
        programLines.add("ARRAY_WRITE(intStack, stackPointer, raxInt)");
        programLines.add("ARRAY_WRITE(floatStack, stackPointer, raxFloat)");
        programLines.add("ARRAY_WRITE(useStack, stackPointer, raxUse)");
        // Increment stack pointer
        programLines.add("ADD32(stackPointer, 1, stackPointer)");
    }

    private void stackMemoryPop() {
        // Move current rax stuffs into rdi stuffs
        programLines.add("MOVE32_32(raxInt, rdiInt)");
        programLines.add("MOVEF_F(raxFloat, rdiFloat)");
        // Decrement stack pointer
        programLines.add("SUB32(stackPointer, 1, stackPointer)");
        // Read from stacks
        programLines.add("ARRAY_READ(intStack, stackPointer, raxInt)");
        programLines.add("ARRAY_READ(floatStack, stackPointer, raxFloat)");
        programLines.add("ARRAY_READ(useStack, stackPointer, raxUse)");
    }

    // Similar to what we did in p4
    private void processExpression() {
        fourthStage();
    }

    // handles "==", "!=", ">=", ">", "<=", "<"
    private void fourthStage() {
        thirdStage();
        while (isFourthStageOperator()) {
            Token.Types t = tokenList.get(tokenIndex).getType();
            tokenIndex++;
            stackMemoryPush();
            thirdStage();
            stackMemoryPop();
            switch (t) {
                case EQEQ: {
                    int ifElse = labelCounter++;
                    programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                    // Set flag for float compare
                    programLines.add("CP_EQF(raxFloat, rdiFloat, compareFlag)");
                    int ifEnd = labelCounter++;
                    programLines.add("JR(LABEL_" + ifEnd + ")");
                    programLines.add("LABEL_" + ifElse + ":");
                    // Set flag for int compare
                    programLines.add("CP_EQ32(raxInt, rdiInt, compareFlag)");
                    programLines.add("LABEL_" + ifEnd + ":");
                }
                case NEQ: {
                    int ifElse = labelCounter++;
                    programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                    // Set flag for float compare
                    programLines.add("CP_NEQF(raxFloat, rdiFloat, compareFlag)");
                    int ifEnd = labelCounter++;
                    programLines.add("JR(LABEL_" + ifEnd + ")");
                    programLines.add("LABEL_" + ifElse + ":");
                    // Set flag for int compare
                    programLines.add("CP_NEQ32(raxInt, rdiInt, compareFlag)");
                    programLines.add("LABEL_" + ifEnd + ":");
                }
                case GEQ: {
                    int ifElse = labelCounter++;
                    programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                    // Set flag for float compare
                    programLines.add("CP_GTEQF(raxFloat, rdiFloat, compareFlag)");
                    int ifEnd = labelCounter++;
                    programLines.add("JR(LABEL_" + ifEnd + ")");
                    programLines.add("LABEL_" + ifElse + ":");
                    // Set flag for int compare
                    programLines.add("CP_GTEQ32(raxInt, rdiInt, compareFlag)");
                    programLines.add("LABEL_" + ifEnd + ":");
                }
                case GT: {
                    int ifElse = labelCounter++;
                    programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                    // Set flag for float compare
                    programLines.add("CP_GTF(raxFloat, rdiFloat, compareFlag)");
                    int ifEnd = labelCounter++;
                    programLines.add("JR(LABEL_" + ifEnd + ")");
                    programLines.add("LABEL_" + ifElse + ":");
                    // Set flag for int compare
                    programLines.add("CP_GT32(raxInt, rdiInt, compareFlag)");
                    programLines.add("LABEL_" + ifEnd + ":");
                }
                case LEQ: {
                    int ifElse = labelCounter++;
                    programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                    // Set flag for float compare
                    programLines.add("CP_LTEQF(raxFloat, rdiFloat, compareFlag)");
                    int ifEnd = labelCounter++;
                    programLines.add("JR(LABEL_" + ifEnd + ")");
                    programLines.add("LABEL_" + ifElse + ":");
                    // Set flag for int compare
                    programLines.add("CP_LTEQ32(raxInt, rdiInt, compareFlag)");
                    programLines.add("LABEL_" + ifEnd + ":");
                }
                case LT: {
                    int ifElse = labelCounter++;
                    programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                    // Set flag for float compare
                    programLines.add("CP_LTF(raxFloat, rdiFloat, compareFlag)");
                    int ifEnd = labelCounter++;
                    programLines.add("JR(LABEL_" + ifEnd + ")");
                    programLines.add("LABEL_" + ifElse + ":");
                    // Set flag for int compare
                    programLines.add("CP_LT32(raxInt, rdiInt, compareFlag)");
                    programLines.add("LABEL_" + ifEnd + ":");
                }
            }
            programLines.add("MOVE8_32(compareFlag, raxInt)");
            programLines.add("MOVE8_8(0, raxUse)");
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

    // handles "+", "-"
    private void thirdStage() {
        secondStage();
        while (isThirdStageOperator()) {
            Token.Types t = tokenList.get(tokenIndex).getType();
            tokenIndex++;
            stackMemoryPush();
            secondStage();
            stackMemoryPop();
            if (t == Token.Types.PLUS) {
                int ifElse = labelCounter++;
                programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                // Float type add
                programLines.add("ADDF(raxFloat, rdiFloat, raxFloat)");
                int ifEnd = labelCounter++;
                programLines.add("JR(LABEL_" + ifEnd + ")");
                programLines.add("LABEL_" + ifElse + ":");
                // Integer type add
                programLines.add("ADD32(raxInt, rdiInt, raxInt)");
                programLines.add("LABEL_" + ifEnd + ":");
            }
            else if (t == Token.Types.MINUS) {
                int ifElse = labelCounter++;
                programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                // Float type subtract
                programLines.add("SUBF(raxFloat, rdiFloat, raxFloat)");
                int ifEnd = labelCounter++;
                programLines.add("JR(LABEL_" + ifEnd + ")");
                programLines.add("LABEL_" + ifElse + ":");
                // Integer type subtract
                programLines.add("SUB32(raxInt, rdiInt, raxInt)");
                programLines.add("LABEL_" + ifEnd + ":");
            }
        }
    }

    private boolean isThirdStageOperator() {
        Token.Types t = tokenList.get(tokenIndex).getType();
        return t == Token.Types.PLUS || t == Token.Types.MINUS;
    }

    // handles "*", "/"
    private void secondStage() {
        firstStage();
        while (isSecondStageOperator()) {
            Token.Types t = tokenList.get(tokenIndex).getType();
            tokenIndex++;
            stackMemoryPush();
            secondStage();
            stackMemoryPop();
            if (t == Token.Types.MUL) {
                int ifElse = labelCounter++;
                programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                // Float type add
                programLines.add("MULF(raxFloat, rdiFloat, raxFloat)");
                int ifEnd = labelCounter++;
                programLines.add("JR(LABEL_" + ifEnd + ")");
                programLines.add("LABEL_" + ifElse + ":");
                // Integer type add
                programLines.add("MUL32(raxInt, rdiInt, raxInt)");
                programLines.add("LABEL_" + ifEnd + ":");
            }
            else if (t == Token.Types.DIV) {
                int ifElse = labelCounter++;
                programLines.add("JR_EQ32(raxUse, 0, LABEL_" + ifElse + ")");
                // Float type subtract
                programLines.add("DIVF(raxFloat, rdiFloat, raxFloat)");
                int ifEnd = labelCounter++;
                programLines.add("JR(LABEL_" + ifEnd + ")");
                programLines.add("LABEL_" + ifElse + ":");
                // Integer type subtract
                programLines.add("DIV32(raxInt, rdiInt, raxInt)");
                programLines.add("LABEL_" + ifEnd + ":");
            }
        }
    }

    private boolean isSecondStageOperator() {
        Token.Types t = tokenList.get(tokenIndex).getType();
        return t == Token.Types.MUL || t == Token.Types.DIV;
    }

    private void firstStage() {
        Token t = tokenList.get(tokenIndex);
        System.out.println("firstStage " + t.toString());
        // Left parentheses and right parentheses case
        if (t.getType() == Token.Types.LPAREN) {
            tokenIndex++;
            processExpression();
            tokenIndex++;
        }
        // Function call or variable
        else if (t.getType() == Token.Types.ID) {
            Token curr = tokenList.get(tokenIndex); tokenIndex++;
            // Break up into cases where we are dealing with a function and are not dealing with a function
            if (functionParameters.containsKey(curr.getName())) {
                generateFunctionCall(curr);
                if (curr.getDataType() == Token.DataTypes.INT) {
                    programLines.add("MOVE32_32(" + curr.getName() + "_ret, raxInt)");
                    programLines.add("MOVE8_8(0, raxUse)");
                }
                else if (curr.getDataType() == Token.DataTypes.FLOAT) {
                    programLines.add("MOVEF_F(" +curr.getName() + "_ret, raxFloat)");
                    programLines.add("MOVE8_8(1, raxUse)");
                }
            }
            else {
                if (curr.getDataType() == Token.DataTypes.INT) {
                    programLines.add("MOVE32_32(" + curr.getName() + ", raxInt)");
                    programLines.add("MOVE8_8(0, raxUse)");
                }
                else if (curr.getDataType() == Token.DataTypes.FLOAT) {
                    programLines.add("MOVEF_F(" +curr.getName() + ", raxFloat)");
                    programLines.add("MOVE8_8(1, raxUse)");
                }
            }
        }
        // Constant int
        else if (t.getValue().isInt()) {
            int x = (int)tokenList.get(tokenIndex).getValue().getValue();
            programLines.add("MOVE32_32(" + x + ", raxInt)");
            programLines.add("MOVE8_8(0, raxUse)");
            tokenIndex++;
        }
        // Constant float
        else if (t.getValue().isFloat()) {
            float x = (float)tokenList.get(tokenIndex).getValue().getValue();
            programLines.add("MOVEF_F(" + x + "F, raxFloat)");
            programLines.add("MOVE8_8(1, raxUse)");
            tokenIndex++;
        }
    }

    // TODO: Add function parameters in the form of tokens just like with normal functions the user makes
    private void generateBuiltInFunctionParameters() {
        // EX: LDRIVE
        // Store expression results in LDRIVE_port, LDRIVE_power, LDRIVE_rotations, LDRIVE_breakAtEnd
        // Then run the actual assembly commands using those variables

    }

    // TODO: Append all pre-established assembly codes to the end of our generated code
    private void addBuiltInFunctionAssembly() {
        // EX: LDRIVE
        // Store expression results in LDRIVE_port, LDRIVE_power, LDRIVE_rotations, LDRIVE_breakAtEnd
        // Then run the actual assembly commands using those variables
    }
}
