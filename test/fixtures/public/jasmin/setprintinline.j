.class public PrintOtherClassInline
.super java/lang/Object

.method public <init>()V
	aload_0
	invokespecial java/lang/Object.<init>()V
	return
.end method

.method public static main([Ljava/lang/String;)V
		.limit locals 5
		.limit stack 3
        
        new GetterAndSetter
        dup
		invokespecial GetterAndSetter
		astore_3

		aload_3
		bipush 10 
		invokevirtual PrintOtherClassInline/setA(I)I
		istore 4

		aload_3
		invokevirtual PrintOtherClassInline/getA()I
        istore 4

		iload 4
		invokestatic io/print(I)V
		return
.end method
