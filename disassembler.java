/*          AUTHORS:
 *  ----------------------
 *        Connor Deahl
 *       Ibrahim Aldualmi
 *        Tristan Prebil
 */

import java.io.*;

public class disassembler {
    public static void main(String[] args) {
        // check if a file was given
        if (args.length < 1) {
            System.out.println("Usage: java disassembler <machine_code_file>");
            return;
        }

        try {
            // read and decode instructions
            int[] instructions = readInstructions(args[0]);
            disassemble(instructions);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // read instructions from the binary file
    private static int[] readInstructions(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        byte[] bytes = fis.readAllBytes();
        fis.close();

        if (bytes.length % 4 != 0) {
            throw new IOException("File size must be multiple of 4 bytes");
        }

        int[] instructions = new int[bytes.length / 4];
        for (int i = 0; i < instructions.length; i++) {
            int offset = i * 4;
            instructions[i] = ((bytes[offset] & 0xFF) << 24) |
                    ((bytes[offset + 1] & 0xFF) << 16) |
                    ((bytes[offset + 2] & 0xFF) << 8) |
                    (bytes[offset + 3] & 0xFF);
        }
        return instructions;
    }

    // disassemble the instructions
    private static void disassemble(int[] instructions) {
        boolean[] isBranchTarget = new boolean[instructions.length];

        // mark branch targets
        for (int i = 0; i < instructions.length; i++) {
            int inst = instructions[i];
            int opcode = getOpcode(inst);

            if (isBranchInstruction(opcode)) {
                int offset = getBranchOffset(inst, opcode);
                int target = i + offset;
                if (target >= 0 && target < instructions.length) {
                    isBranchTarget[target] = true;
                }
            }
        }

        // print disassembled code with labels
        for (int i = 0; i < instructions.length; i++) {
            if (isBranchTarget[i]) {
                System.out.println("Label" + i + ":");
            }
            String asm = decodeInstruction(instructions[i], i);
            System.out.println("    " + asm);
        }
    }

    // get opcode from instruction
    private static int getOpcode(int inst) {
        int opcode = (inst >>> 21) & 0x7FF;


        if (((opcode >>> 3) == 0x54) || ((opcode >>> 3) == 0xB4) || ((opcode >>> 3) == 0xB5)) {
            return (inst >>> 24) & 0xFF;
        }
        if ((opcode >>> 5) == 0x5 || (opcode >>> 5) == 0x25) {
            return (inst >>> 26) & 0x3F;
        }
        return opcode;
    }

    // decode instruction and return it as string
    private static String decodeInstruction(int inst, int instrCounter) {
        int opcode = getOpcode(inst);

        switch (opcode) {
            case 0x450: return formatR("AND", inst);
            case 0x458: return formatR("ADD", inst);
            case 0x4D8: return formatR("MUL", inst);
            case 0x550: return formatR("ORR", inst);
            case 0x650: return formatR("EOR", inst);
            case 0x658: return formatR("SUB", inst);
            case 0x69A: return formatShift("LSR", inst);
            case 0x69B: return formatShift("LSL", inst);
            case 0x6B0: return "BR " + isZeroReg((inst >>> 5) & 0x1F);
            case 0x758: return formatR("SUBS", inst);
            case 0x7C0: return formatD("STUR", inst);
            case 0x7C2: return formatD("LDUR", inst);
            case 0x488: return formatI("ADDI", inst);
            case 0x490: return formatI("ANDI", inst);
            case 0x590: return formatI("ORRI", inst);
            case 0x688: return formatI("SUBI", inst);
            case 0x690: return formatI("EORI", inst);
            case 0x788: return formatI("SUBIS", inst);
            case 0x5: return formatB("B", inst, instrCounter);
            case 0x25: return formatB("BL", inst, instrCounter);
            case 0x54: return formatCondB(inst, instrCounter);
            case 0xB4: return formatCB("CBZ", inst, instrCounter);
            case 0xB5: return formatCB("CBNZ", inst, instrCounter);
            case 0x7FC: return "PRNL";
            case 0x7FD: return "PRNT " + isZeroReg(inst & 0x1F);
            case 0x7FE: return "DUMP";
            case 0x7FF: return "HALT";
            default: return "";
        }
    }

    // -------- Formatting functions --------

    // Format R-type: uses 3 registers
    private static String formatR(String instrName, int inst) {
        int rd = inst & 0x1F;               // destination reg
        int rn = (inst >>> 5) & 0x1F;        // first source reg
        int rm = (inst >>> 16) & 0x1F;       // second source reg
        return String.format("%s %s, %s, %s", instrName, isZeroReg(rd), isZeroReg(rn), isZeroReg(rm));
    }

    // Format shift instructions (like LSR, LSL)
    private static String formatShift(String instrName, int inst) {
        int rd = inst & 0x1F;               // destination reg
        int rn = (inst >>> 5) & 0x1F;        // source reg
        int shamt = (inst >>> 10) & 0x3F;    // shift amount
        return String.format("%s %s, %s, #%d", instrName, isZeroReg(rd), isZeroReg(rn), shamt);
    }

    // Format D-type: load/store with offset
    private static String formatD(String instrName, int inst) {
        int rt = inst & 0x1F;               // target reg
        int rn = (inst >>> 5) & 0x1F;        // base reg
        int dtAddress = (inst >>> 12) & 0x1FF;  // offset
        dtAddress = (dtAddress << 23) >> 23;    // sign extend
        return String.format("%s %s, [%s, #%d]", instrName, isZeroReg(rt), isZeroReg(rn), dtAddress);
    }

    // Format I-type: uses immediate number
    private static String formatI(String instrName, int inst) {
        int rd = inst & 0x1F;               // destination reg
        int rn = (inst >>> 5) & 0x1F;        // source reg
        int immediate = (inst >>> 10) & 0xFFF;  // immediate value
        return String.format("%s %s, %s, #%d", instrName, isZeroReg(rd), isZeroReg(rn), immediate);
    }

    // Format B-type: unconditional branch
    private static String formatB(String instrName, int inst, int instrCounter) {
        int offset = getBranchOffset(inst, 0x5);   // branch offset
        return String.format("%s Label%d", instrName, instrCounter + offset);
    }

    // Format CB-type: branch if reg == 0 or != 0
    private static String formatCB(String instrName, int inst, int instrCounter) {
        int rt = inst & 0x1F;                    // reg to check
        int offset = getBranchOffset(inst, 0xB5); // branch offset
        return String.format("%s X%d, Label%d", instrName, rt, instrCounter + offset);
    }

    // Format B.cond: branch if condition met
    private static String formatCondB(int inst, int instrCounter) {
        String[] conditions = {"EQ", "NE", "HS", "LO", "MI", "PL", "VS", "VC",
                "HI", "LS", "GE", "LT", "GT", "LE"};
        int cond = inst & 0xF;                    // condition code
        int offset = getBranchOffset(inst, 0x54); // branch offset
        String condStr;
        if (cond < conditions.length) {
            condStr = conditions[cond];
        } else {
            condStr = "?";
        }
        return String.format("B.%s Label%d", condStr, instrCounter + offset);
    }

    // -------- Helper functions --------

    // check if it's XZR (reg 31) or normal register
    private static String isZeroReg(int regNum) {
        return (regNum == 31) ? "XZR" : "X" + regNum;
    }

    // check if instruction is a branch
    private static boolean isBranchInstruction(int opcode) {
        return (opcode == 0x5 || opcode == 0x25 || opcode == 0x54 || opcode == 0xB4 || opcode == 0xB5);
    }

    // get the branch offset from instruction
    private static int getBranchOffset(int inst, int opcode) {
        int offset;
        if (opcode == 0x5 || opcode == 0x25) { // B or BL
            offset = inst & 0x3FFFFFF;
            if ((offset & 0x2000000) != 0) {
                offset |= 0xFC000000; // sign extend
            }
        } else { // CBZ, CBNZ, B.cond
            offset = (inst >> 5) & 0x7FFFF;
            if ((offset & 0x40000) != 0) {
                offset |= 0xFFF80000; // sign extend
            }
        }
        return offset;
    }
}
