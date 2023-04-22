package ve.edu.unet;

import ve.edu.unet.nodosAST.*;

public class Generador {
	private static int desplazamientoTmp = 0;
	private static TablaSimbolos tablaSimbolos = null;

	public static void setTablaSimbolos(TablaSimbolos tabla){
		tablaSimbolos = tabla;
	}

	public static void generarCodigoObjeto(NodoBase raiz){
		System.out.println();
		System.out.println();
		System.out.println("------ CODIGO OBJETO DEL LENGUAJE P GENERADO ------");
		System.out.println();
		System.out.println();
		generarPreludioEstandar();
		generar(raiz);
		UtGen.emitirComentario("Fin de la ejecucion.");
		UtGen.emitirRO("HALT", 0, 0, 0, "");
		System.out.println();
		System.out.println();
		System.out.println("------ FIN DEL CODIGO OBJETO DEL LENGUAJE P GENERADO ------");
	}

	private static void generar(NodoBase nodo){
		if(tablaSimbolos!=null){
			if (nodo instanceof  NodoIf){
				generarIf(nodo);
			}else if (nodo instanceof  NodoRepeat){
				generarRepeat(nodo);
			}else if (nodo instanceof  NodoAsignacion){
				generarAsignacion(nodo);
			}else if (nodo instanceof  NodoLeer){
				generarLeer(nodo);
			}else if (nodo instanceof  NodoEscribir){
				generarEscribir(nodo);
			}else if (nodo instanceof NodoValor){
				generarValor(nodo);
			}else if (nodo instanceof NodoIdentificador){
				generarIdentificador(nodo);
			}else if (nodo instanceof NodoOperacion){
				generarOperacion(nodo);
			}else{
				System.out.println("BUG: Tipo de nodo a generar desconocido");
			}
			if(nodo.TieneHermano())
				generar(nodo.getHermanoDerecha());
		}else
			System.out.println("   ERROR: por favor fije la tabla de simbolos a usar antes de generar codigo objeto!!!");
	}

	private static void generarIf(NodoBase nodo){
		NodoIf n = (NodoIf) nodo;
		int etiqueta1, etiqueta2;

		/* Genera código para la expresión de prueba */
		generar(n.getPrueba());

		etiqueta1 = UtGen.emitirSalto(1);
		UtGen.emitirComentario("If: salto si es falso");

		/* Genera código para la parte THEN */
		generar(n.getParteThen());

		etiqueta2 = UtGen.emitirSalto(1);
		UtGen.emitirComentario("If: salto incondicional");

		UtGen.cargarRespaldo(etiqueta1);
		UtGen.emitirRM_Abs("JEQ", UtGen.AC, UtGen.emitirSalto(0), "if: jmp hacia else");
		UtGen.restaurarRespaldo();

		/* Genera código para la parte ELSE, si la hay */
		if (n.getParteElse() != null) {
			generar(n.getParteElse());
		}

		UtGen.cargarRespaldo(etiqueta2);
		UtGen.emitirRM_Abs("LDA", UtGen.PC, UtGen.emitirSalto(0), "if: jmp hacia el final");
		UtGen.restaurarRespaldo();
	}

	private static void generarRepeat(NodoBase nodo){
		NodoRepeat n = (NodoRepeat) nodo;
		int inicio = UtGen.emitirSalto(0);
		UtGen.emitirComentario("Repeat: inicio");

		/* Genera código para el cuerpo del bucle */
		generar(n.getCuerpo());

		/* Genera código para la expresión de prueba */
		generar(n.getPrueba());

		UtGen.emitirRM_Abs("JNE", UtGen.AC, inicio, "repeat: jmp hacia el inicio del cuerpo(esto esta mal)");
		UtGen.emitirComentario("Repeat: fin");
	}

	private static void generarAsignacion(NodoBase nodo){
		NodoAsignacion n = (NodoAsignacion) nodo;

		/* Genera código para la expresión */
		generar(n.getExpresion());

		int direccion = tablaSimbolos.getDireccion(n.getIdentificador());
		UtGen.emitirRM("ST", UtGen.AC, direccion, UtGen.GP, "Asignacion: almacena el resultado");
	}

	private static void generarLeer(NodoBase nodo){
		NodoLeer n = (NodoLeer) nodo;
		int direccion = tablaSimbolos.getDireccion(n.getIdentificador());

		UtGen.emitirRO("IN", UtGen.AC, 0, 0, "Leer: lee un entero");
		UtGen.emitirRM("ST", UtGen.AC, direccion, UtGen.GP, "Leer: almacena el valor leido");
	}

	private static void generarEscribir(NodoBase nodo){
		NodoEscribir n = (NodoEscribir) nodo;

		/* Genera código para la expresión */
		generar(n.getExpresion());

		UtGen.emitirRO("OUT", UtGen.AC, 0, 0, "Escribir: escribe el resultado");
	}

	private static void generarValor(NodoBase nodo){
		NodoValor n = (NodoValor) nodo;

		UtGen.emitirRM("LDC", UtGen.AC, n.getValor(), 0, "Carga una constante");
	}

	private static void generarIdentificador(NodoBase nodo){
		NodoIdentificador n = (NodoIdentificador) nodo;
		int direccion = tablaSimbolos.getDireccion(n.getNombre());

		UtGen.emitirRM("LD", UtGen.AC, direccion, UtGen.GP, "Carga el valor del identificador");
	}

	private static void generarOperacion(NodoBase nodo){
		NodoOperacion n = (NodoOperacion) nodo;

		/* Genera código para el operando izquierdo */
		generar(n.getOpIzquierdo());

		/* Guarda el resultado temporalmente */
		UtGen.emitirRM("ST", UtGen.AC, desplazamientoTmp, UtGen.MP, "Operacion: guarda temporalmente el operando izquierdo");

		/* Genera código para el operando derecho */
		generar(n.getOpDerecho());

		/* Realiza la operación */
		switch (n.getOperacion()) {
			case mas:
				UtGen.emitirRM("LD", UtGen.AC1, desplazamientoTmp, UtGen.MP, "Operacion: carga el operando izquierdo");
				UtGen.emitirRO("ADD", UtGen.AC, UtGen.AC1, UtGen.AC, "Operacion: suma");
				break;
			case menos:
				UtGen.emitirRM("LD", UtGen.AC1, desplazamientoTmp, UtGen.MP,"Operacion: carga el operando izquierdo");
				UtGen.emitirRO("SUB", UtGen.AC, UtGen.AC1, UtGen.AC, "Operacion: resta");
				break;
			case por:
				UtGen.emitirRM("LD", UtGen.AC1, desplazamientoTmp, UtGen.MP, "Operacion: carga el operando izquierdo");
				UtGen.emitirRO("MUL", UtGen.AC, UtGen.AC1, UtGen.AC, "Operacion: multiplicacion");
				break;
			case entre:
				UtGen.emitirRM("LD", UtGen.AC1, desplazamientoTmp, UtGen.MP, "Operacion: carga el operando izquierdo");
				UtGen.emitirRO("DIV", UtGen.AC, UtGen.AC1, UtGen.AC, "Operacion: division");
				break;
			case igual:
				UtGen.emitirRM("LD", UtGen.AC1, desplazamientoTmp, UtGen.MP, "Operacion: carga el operando izquierdo");
				UtGen.emitirRO("SUB", UtGen.AC, UtGen.AC1, UtGen.AC, "Operacion: resta");
				UtGen.emitirRO("JEQ", UtGen.AC, 0, 2, "Operacion: salta si son iguales");
				UtGen.emitirRM("LDC", UtGen.AC, 0, 0, "Operacion: carga falso");
				UtGen.emitirRM("LDA", UtGen.PC, 1, UtGen.PC, "Operacion: salta incondicionalmente");
				UtGen.emitirRM("LDC", UtGen.AC, 1, 0, "Operacion: carga verdadero");
				break;
			case menor:
				UtGen.emitirRM("LD", UtGen.AC1, desplazamientoTmp, UtGen.MP, "Operacion: carga el operando izquierdo");
				UtGen.emitirRO("SUB", UtGen.AC, UtGen.AC1, UtGen.AC, "Operacion: resta");
				UtGen.emitirRO("JLT", UtGen.AC, 0, 2, "Operacion: salta si es menor");
				UtGen.emitirRM("LDC", UtGen.AC, 0, 0, "Operacion: carga falso");
				UtGen.emitirRM("LDA", UtGen.PC, 1, UtGen.PC, "Operacion: salta incondicionalmente");
				UtGen.emitirRM("LDC", UtGen.AC, 1, 0, "Operacion: carga verdadero");
				break;
			default:
				System.out.println("BUG: Operador desconocido");
				break;
		}
	}

	//TODO: enviar preludio a archivo de salida, obtener antes su nombre
	private static void generarPreludioEstandar(){
		UtGen.emitirComentario("Compilacion P para el codigo objeto");
		UtGen.emitirComentario("Archivo: "+ "NOMBRE_ARREGLAR");
		/*Genero inicializaciones del preludio estandar*/
		/*Todos los registros en P comienzan en cero*/
		UtGen.emitirComentario("Preludio estandar:");
		UtGen.emitirRM("LD", UtGen.MP, 0, UtGen.AC, "cargar la maxima direccion desde la localidad 0");
		UtGen.emitirRM("ST", UtGen.AC, 0, UtGen.AC, "limpio el registro de la localidad 0");
	}

}
