    ADDI X1, XZR, #5        
    ADDI X2, XZR, #3        
    ADDI X3, XZR, #1        
    ADDI X4, XZR, #10       

Label1:
    // Arithmetic tests
    ADD X5, X1, X2        
    SUB X6, X1, XZR        
    MUL X7, X1, X3      

    // Memory stuff
    STUR X5, [XZR, #0]   
    LDUR X8, [XZR, #0]  

    // Conditional branch
    CBZ X3, Label3          
    SUBIS X9, X4, #5        
    B.GT Label2             

Label3:
    // Bitwise stuff
    AND X10, X1, X2         
    ORR X11, X1, X2         
    EOR X12, X1, XZR        

    // Shift and print
    LSL X13, X1, #2         
    PRNT X13                
    PRNL

    BL _function
    B Label1

Label2:
    // Different path
    EORI X14, X1, #15
    CBNZ X14, Label4

_function:
    ADDI X15, XZR, #42
    BR XZR 

Label4:
    // Given stuff, not in sheet
    ANDI X16, X1, #1 
    PRNT X16
    PRNL
    DUMP
    HALT