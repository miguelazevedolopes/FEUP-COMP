.class public Fac
.super java/lang/Object


.method public <init>()V
	aload_0
	invokespecial java/lang/Object.<init>()V
	return
.end method

.method public compFac(I)I
		.limit locals 5
		.limit stack 2

		iload_1
		iconst_1 
		if_icmpge else
		iconst_1 
		istore_2

		goto endif

	else:
		iload_1
		iconst_1 
		isub
		istore_3

		aload_0
		iload_3
		invokevirtual Fac/compFac(I)I
		istore 4

		iload_1
		iload 4
		imul
		istore_2

	endif:
		iload_2
		
		ireturn
.end method

.method public static main([Ljava/lang/String;)V
		.limit locals 4
		.limit stack 4

		new Fac
		dup
		invokespecial Fac.<init>()V
		astore_2

		aload_2
		bipush 10 
		invokevirtual Fac.compFac(I)I
		istore_3

		iload_3
		invokestatic io/println(I)V
.end method