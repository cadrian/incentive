package net.cadrian.incentive.assist;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.compiler.CompileError;
import javassist.compiler.TokenId;
import javassist.compiler.TypeChecker;
import javassist.compiler.ast.ASTree;

class StmtTypeVisitor extends TypeChecker implements TokenId {

	StmtTypeVisitor(final CtClass a_targetClass, final ClassPool a_pool) {
		super(a_targetClass, a_pool);
	}

	public CtClass getType(final ASTree astree) throws CompileError {
		final CtClass result;

		astree.accept(this);

		final CtClass clazz = resolver.lookupClass(exprType, arrayDim,
				className);
		if (clazz instanceof CtPrimitiveType) {
			switch (exprType) {
			case BOOLEAN:
				result = CtClass.booleanType;
				break;
			case BYTE:
				result = CtClass.byteType;
				break;
			case CHAR:
				result = CtClass.charType;
				break;
			case DOUBLE:
				result = CtClass.doubleType;
				break;
			case FLOAT:
				result = CtClass.floatType;
				break;
			case INT:
				result = CtClass.intType;
				break;
			case LONG:
				result = CtClass.longType;
				break;
			case SHORT:
				result = CtClass.shortType;
				break;
			case VOID:
				result = CtClass.shortType;
				break;
			default:
				result = clazz;
			}
		} else {
			result = clazz;
		}

		return result;
	}

}
