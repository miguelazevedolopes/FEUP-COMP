.class public myClass
.super java/lang/Object


.method public <init>()V
	aload_0
	invokespecial java/lang/Object.<init>()V
	return
.end method

.method public sum([I)I
		.limit locals 6
		.limit stack 5

		iconst_0 
		istore_2

		iconst_0 
		istore_3

	Loop:
		aload_1
		arraylength
		istore 4

		iload_3
		iload 4
		if_icmpge End
		aload_1
		iload_3
		iaload
		istore 5

		iload_2
		iload 5
		iadd
		istore_2

		iinc 3 1
		goto Loop

	End:
		iload_2
		
		ireturn
.end method