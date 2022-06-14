.class public Arithmetic_and
.super java/lang/Object


.method public <init>()V
	aload_0
	invokespecial java/lang/Object.<init>()V
	return
.end method

.method public static main([Ljava/lang/String;)V
		.limit locals 5
		.limit stack 2

        iconst_0
        istore_1
		iload_1
		ifne ifbody_0

		iconst_0 
		invokestatic io/print(I)V
		goto endif_0

	ifbody_0:
		iconst_1 
		invokestatic io/print(I)V
	endif_0:
		return
.end method