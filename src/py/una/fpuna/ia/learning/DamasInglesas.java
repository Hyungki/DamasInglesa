/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.una.fpuna.ia.learning;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DamasInglesas {

    private HashMap<String, Double> lookupTable;
    private int[][] tablero;
    private double alpha;
    private boolean entrenar;
    private int gameResult;
    private int N;
    private int[][] lastTablero; //para guardar el estado anterior donde estuvo el agente
    private final int jugadorAgente = 1;
    private double qRate = 0.1;
    private final int size = 8;
    private Lista reinasA, reinasB;
    private int fichasA, fichasB;
    private final int fwrdA = -1, fwrdB = 1;
    boolean noMovA, noMovB;

    public DamasInglesas(int N) {

        this.N = N;
        lookupTable = new HashMap<String, Double>();
        reset(true);
//        this.printTablero();
//        System.exit(0);
    }

    public void reset(boolean entrenar) { // 3 primeras filas enemigo, 3 ultimas filas agente.

        tablero = new int[8][8];
        lastTablero = new int[8][8];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i < 3) { // fichas del contrario
                    if ((i % 2 == 0 && j % 2 != 0) || (i % 2 != 0 && j % 2 == 0)) {
                        tablero[i][j] = 2;
                    } else {
                        tablero[i][j] = 0;
                    }
                } else if (i > 4) { // fichas del agente
                    if ((i % 2 != 0 && j % 2 == 0) || (i % 2 == 0 && j % 2 != 0)) {
                        tablero[i][j] = 1;
                    } else {
                        tablero[i][j] = 0;
                    }
                } else {
                    tablero[i][j] = 0;
                }
            }
        }
        this.entrenar = entrenar;
        this.gameResult = 0;
        this.fichasA = 12;
        this.fichasB = 12;
        this.noMovA = false;
        this.noMovB = false;
        this.reinasA = new Lista();
        this.reinasB = new Lista();
    }

    private int calculateResult(int[][] tablero) {

        //1:gana jugador x
        //2:gana jugador o
        //3:empate
        //0:no hay ganador
        int jugador = 1;
        int contrario = 2;
        
        if (this.fichasB == 0 || this.noMovB) return jugador;
        else if (this.fichasA == 0 || this.noMovA) return contrario;
//        else if (this.noMovA || this.noMovB) return 3; // empate
        else return 0; // continua el juego
    }

    private double calculateReward(int[][] tablero, int jugador) {

        //(1 % 2) + 1 = 2
        //(2 % 2) + 1 = 1
        int contrario = (jugador % 2) + 1;

        int result = calculateResult(tablero);
        if (result == jugador) {
            System.out.println("Gana agente");
            return 1.0;
        } else if (result == contrario) {
            System.out.println("Pierde agente");
            return 0.0;
//        } else if (result == 3) {//empate
//            System.out.println("Empata");
//            return 0.0;
        } else {//no hay ganador

            return getProbability(tablero);
        }
    }

    private double getProbability(int[][] tablero) {

        String tableroSerializado = "";
        for (int i = 0; i < tablero.length; i++) {
            for (int j = 0; j < tablero[0].length; j++) {

                tableroSerializado += tablero[i][j];
            }
        }
        //si aun no contiene la tabla, insertar con valor inicial 0.5
        if (!lookupTable.containsKey(tableroSerializado)) {
            lookupTable.put(tableroSerializado, 0.5);
        }

        return lookupTable.get(tableroSerializado);
    }

    private String serializarTablero(int[][] tablero) {

        String tableroSerializado = "";
        for (int i = 0; i < tablero.length; i++) {
            for (int j = 0; j < tablero[0].length; j++) {

                tableroSerializado += tablero[i][j];
            }
        }

        return tableroSerializado;
    }

    private int[][] deserializar(String tableroSerializado) {

        int valor;
        int[][] tablero = new int[this.size][this.size];
        for (int i = 0; i < tableroSerializado.length(); i++) {

            valor = Integer.parseInt(tableroSerializado.charAt(i) + "");
            tablero[i / this.size][i % this.size] = valor;
        }

        return tablero;
    }

    private void updateProbability(int[][] tablero, double nextStateProb, int jugador) {

        double prob = calculateReward(tablero, jugador);
        //if(lookupTable.containsKey(tableroSerializado))
        //	prob = lookupTable.get(tableroSerializado);		

        prob = prob + alpha * (nextStateProb - prob);

        String tableroSerializado = serializarTablero(tablero);
        lookupTable.put(tableroSerializado, prob);
    }

    private void swap(int i, int j, int k, int l) {
        int aux = this.tablero[i][j];
        this.tablero[i][j] = this.tablero[k][l];
        this.tablero[k][l] = aux;
    }

    private void jugar(int jugador) {

        double prob;
        int row = 0, column = 0, oldRow = -1, oldCol = -1;
        double maxProb = Integer.MIN_VALUE;
        int contrario = (this.jugadorAgente % 2) + 1;
        boolean sePudoComer = false, sePudoMover = false;
        
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                
                if (tablero[i][j] == jugador) { // sea un PEON o REINA indistintamente, evaluar movimiento hacia adelante
                    
                    /* Indices para saltar y comer hacia izquierda y derecha. */
                    int k = i+2*this.fwrdA; // fila a la que se salta (disminuye fila)
                    int l = j-2;            // columna a la que se salta para la izquierda
                    int ll = j+2;           // columna a la que se salta para la derecha
                    
                    if (isInRange(k, l)) { // evitar desbordamiento
                        
                        if (this.tablero[k][l] == 0 && this.tablero[k+1][l+1] == contrario) { // intentar comer, norOeste
                            
                            // salta dos casillas y come
                            this.swap(i, j, k, l);
                            this.tablero[k+1][l+1] = 0;
                            // actualiza cantidad de fichas del enemigo
                            this.fichasB--;
                            
                            /* Evaluar deseabilidad. */
                            prob = calculateReward(tablero, jugador);
                            if(prob > maxProb) {
                                    maxProb = prob;
                                    row = k;
                                    column = l;
                                    oldRow = i;
                                    oldCol = j;
                                    sePudoComer = true;
                            }
                            
                            /* Restablecer los cambios para siguiente evaluacion. */
                            this.swap(i, j, k, l);
                            this.tablero[k+1][l+1] = contrario;
                            this.fichasB++;
                            
                        }
                    }
                    
                    if (isInRange(k, ll)) { // evitar desbordamiento
                        
                        if (this.tablero[k][ll] == 0 && this.tablero[k+1][ll-1] == contrario) { // intentar comer, norEste
                            
                            this.swap(i, j, k, ll);
                            this.tablero[k+1][ll-1] = 0;
                            this.fichasB--;
                            
                            /* Evaluar deseabilidad. */
                            prob = calculateReward(tablero, jugador);
                            if(prob > maxProb) {
                                    maxProb = prob;
                                    row = k;
                                    column = ll;
                                    oldRow = i;
                                    oldCol = j;
                                    sePudoComer = true;
                            }
                            
                            /* Restablecer los cambios para siguiente evaluacion. */
                            this.swap(i, j, k, ll);
                            this.tablero[k+1][ll-1] = contrario;
                            this.fichasB++;
                        }
                    }
                    
                    /* si la ficha en analisis es una REINA,
                    evaluar movimientos hacia atras tambien. */
                    if (this.reinasA.contains(i,j)) {
                        
                        k = i-2*this.fwrdA; // movimiento en retroceso (aumenta fila)
                        l = j-2;
                        ll = j+2;
                        
                        if (isInRange(k,l)) { // evitar desbordamiento
                            
                            if (tablero[k][l]==0 && tablero[k-1][l+1]==contrario) { // intentar comer, surOeste
                                
                                this.swap(i, j, k, l);
                                this.tablero[k-1][l+1] = 0;
                                this.fichasB--;
                                
                                /* Evaluar deseabilidad. */
                                prob = calculateReward(tablero, jugador);
                                if(prob > maxProb) {
                                    maxProb = prob;
                                    row = k;
                                    column = l;
                                    oldRow = i;
                                    oldCol = j;
                                    sePudoComer = true;
                                }
                                
                                /* Restablecer los cambios para siguiente evaluacion. */
                                this.swap(i, j, k, l);
                                this.tablero[k-1][l+1] = contrario;
                                this.fichasB++;
                            }
                        }
                            
                        if (isInRange(k,ll)) { // evitar desbordamiento

                            if (this.tablero[k][ll] == 0 && this.tablero[k-1][ll-1] == contrario) { // intentar comer, surEste

                                this.swap(i, j, k, ll);
                                this.tablero[k-1][ll-1] = 0;
                                this.fichasB--;

                                /* Evaluar deseabilidad. */
                                prob = calculateReward(tablero, jugador);
                                if(prob > maxProb) {
                                        maxProb = prob;
                                        row = k;
                                        column = ll;
                                        oldRow = i;
                                        oldCol = j;
                                        sePudoComer = true;
                                }

                                /* Restablecer los cambios para siguiente evaluacion. */
                                this.swap(i, j, k, ll);
                                this.tablero[k-1][ll-1] = contrario;
                                this.fichasB++;
                            }
                        }
                    }
                    
                    if (!sePudoComer) { // probar movimientos diagonales adyacentes SII no se pudo comer al oponente
                        
                        /* Indices para moverse hacia casillas diagonales adyecentes. */
                        k = i+this.fwrdA; // avanzar en la fila (disminuye fila)
                        l = j-1;          // moverse hacia la izquierda
                        ll = j+1;         // moverse hacia la derecha
                        
                        if (isInRange(k, l) && this.tablero[k][l] == 0) { // moverse hacia norOeste
                            this.swap(i, j, k, l);
                            prob = calculateReward(tablero, jugador);
                            if(prob > maxProb) {
                                    maxProb = prob;
                                    row = k;
                                    column = l;
                                    oldRow = i;
                                    oldCol = j;
                            }
                            /* Restablecer los cambios para siguiente evaluacion. */
                            this.swap(i, j, k, l);
                            sePudoMover = true;
                        }
                        
                        if (isInRange(k,ll) && this.tablero[k][ll] == 0) { // moverse hacia norEste
                            this.swap(i, j, k, ll);
                            prob = calculateReward(tablero, jugador);
                            if(prob > maxProb) {
                                    maxProb = prob;
                                    row = k;
                                    column = ll;
                                    oldRow = i;
                                    oldCol = j;
                            }
                            /* Restablecer los cambios para siguiente evaluacion. */
                            this.swap(i, j, k, ll);
                            sePudoMover = true;
                        }
                        
                        // si es reina, probar movimientos adyacentes hacia atras tambien
                        if (this.reinasA.contains(i,j)) {
                            /* Indices para moverse hacia casillas diagonales adyecentes. */
                            k = i-this.fwrdA; // retroceder en la fila (aumenta fila)
                            l = j-1;          // moverse hacia la izquierda
                            ll = j+1;         // moverse hacia la derecha

                            if (isInRange(k,l) && this.tablero[k][l] == 0) { // moverse hacia surOeste
                                this.swap(i, j, k, l);
                                prob = calculateReward(tablero, jugador);
                                if(prob > maxProb) {
                                        maxProb = prob;
                                        row = k;
                                        column = l;
                                        oldRow = i;
                                        oldCol = j;
                                }
                                /* Restablecer los cambios para siguiente evaluacion. */
                                this.swap(i, j, k, l);
                                sePudoMover = true;
                            }

                            if (isInRange(k,ll) && this.tablero[k][ll] == 0) { // moverse hacia surEste
                                this.swap(i, j, k, ll);
                                prob = calculateReward(tablero, jugador);
                                if(prob > maxProb) {
                                        maxProb = prob;
                                        row = k;
                                        column = ll;
                                        oldRow = i;
                                        oldCol = j;
                                }
                                /* Restablecer los cambios para siguiente evaluacion. */
                                this.swap(i, j, k, ll);
                                sePudoMover = true;
                            }
                        }
                    }
                }
            }
        }

        //entrenar
//        if (entrenar) {
//            updateProbability(lastTablero, maxProb, jugador);
//        }

        // aplicar jugada si alguna ficha cambio de lugar
        if (sePudoComer || sePudoMover) {
            
            //entrenar
            if (entrenar) {
                updateProbability(lastTablero, maxProb, jugador);
            }

            this.swap(oldRow, oldCol, row, column); // efectua movimiento
            
            boolean reinaExistente = this.reinasA.contains(oldRow,oldCol);
            // si la ficha movida fue una reina, o es una nueva reina
            if (reinaExistente || row == 0) {
                // registrar la nueva posicion de la reina en la lista.
                this.reinasA.add(row,column);

                // si la ficha movida era una reina ya existente
                if (reinaExistente) {
                    // borrar el registro de su antigua posicion
                    this.reinasA.remove(oldRow,oldCol);
                    System.out.println("Es una reina del agente");
                } else
                    System.out.println("Es una NUEVA reina del agente");
            }

            // si comio alguna ficha del oponente
            if(sePudoComer) {

                int iEliminada = oldRow-(oldRow-row)/2,
                    jEliminada = oldCol-(oldCol-column)/2;
                this.tablero[iEliminada][jEliminada] = 0;
                this.fichasB--;

                // si era una reina del oponente
                if (this.reinasB.contains(iEliminada,jEliminada))
                    this.reinasB.remove(iEliminada,jEliminada);
            }
        } else {
            this.noMovA = true;
        }

        //actualizar ultimo tablero
        this.copiarTablero(tablero, lastTablero);
    }
    
    private boolean isInRange(int i, int j) {
        return (i>-1 && i<this.size) && (j>-1 && j<this.size);
    }
    
    private void jugarRandom(int jugador) {

        ArrayList<Integer> row = new ArrayList<Integer>();
        ArrayList<Integer> col = new ArrayList<Integer>();
        List<int[]> origRowCol = new ArrayList<int[]>();
        ArrayList<Boolean> huboCaptura = new ArrayList<Boolean>();
        int noAgente = 2;
        boolean huboMov = false, hayPosibleMov = false;
        
        /* TODO EL ANALISIS DENTRO DE LOS 2 FOR Y DENTRO DE 479, DEBE ESTAR
        COMPROBADO TANTO PARA AGENTE COMO PARA OPONENTE.*/
        
        for (int i = 0; i < tablero.length; i++) {
            for (int j = 0; j < tablero[0].length; j++) {
                
                int k, l, m;
                huboMov = false;

                if (tablero[i][j] == jugador && jugador == this.jugadorAgente) { // si es el agente

                    k = i-2; l=j-2; m=j+2;

                    // intentar comer, noroeste.                        
                    if (isInRange(k,l) && tablero[k][l] == 0 && tablero[k+1][l+1] == noAgente) {
                        row.add(k); col.add(l);  // guardar la fila y columna hacia el cual podria moverse.
                        origRowCol.add(new int[]{i,j}); // guardar la fila y columna desde el cual podria moverse.
                        huboCaptura.add(true);          // guardar indicador sobre si habra captura de ficha del contrario en la posible movida.
                        huboMov = true;                 // indica si ocurrio captura de ficha del contrario.
                        hayPosibleMov = true;
                    }

                    // intentar comer, noreste
                    if (isInRange(k,m) && tablero[k][m] == 0 && tablero[k+1][m-1] == noAgente) {
                        row.add(k); col.add(m);
                        origRowCol.add(new int[]{i,j});
                        huboCaptura.add(true);
                        huboMov = true;
                        hayPosibleMov = true;
                    }

                    k = i+2;
                   
                    // la ficha a moverse es una reina
                    if (this.reinasA.contains(i,j)) {
                        
                        // intentar comer, suroeste
                        if (isInRange(k,l) && tablero[k][l] == 0 && tablero[k-1][l+1] == noAgente) {
                            row.add(k); col.add(l);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(true);
                            huboMov = true;
                            hayPosibleMov = true;
                        }
                        
                        // intentar comer, sureste
                        if (isInRange(k,m) && tablero[k][m] == 0 && tablero[k-1][m-1] == noAgente) {
                            row.add(k); col.add(m);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(true);
                            huboMov = true;
                            hayPosibleMov = true;
                        }
                    }

                    /* si no existe ningun movimiento en donde pueda realizarse 
                    captura alguna, evaluar movimiento a casillas diagonales 
                    adyacentes. */
                    if (!huboMov) {

                        k = i-1; l=j-1; m=j+1;

                        // moverse a noroeste
                        if (isInRange(k, l) && tablero[k][l] == 0) {
                            row.add(k); col.add(l);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(false);
                            huboMov = true;
                            hayPosibleMov = true;
                        }

                        // moverse al noreste
                        if (isInRange(k, m) && tablero[k][m] == 0) {
                            row.add(k); col.add(m);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(false);
                            huboMov = true;
                            hayPosibleMov = true;
                        }
                        
                        // la ficha a moverse es una reina
                        if (!huboMov && this.reinasA.contains(i,j)) {
                            
                            k = i+1;

                            // moverse a suroeste
                            if (isInRange(k, l) && tablero[k][l] == 0) {
                                row.add(k); col.add(l);
                                origRowCol.add(new int[]{i,j});
                                huboCaptura.add(false);
                                huboMov = true;
                                hayPosibleMov = true;
                            }

                            // moverse al sureste
                            if (isInRange(k, m) && tablero[k][m] == 0) {
                                row.add(k); col.add(m);
                                origRowCol.add(new int[]{i,j});
                                huboCaptura.add(false);
                                huboMov = true;
                                hayPosibleMov = true;
                            }
                        }
                    }
                } else if (tablero[i][j]==jugador && jugador==noAgente) {
                    
                    k = i+2; l=j-2; m=j+2;

                    // intentar comer, SUROESTE.                        
                    if (isInRange(k,l) && tablero[k][l] == 0 && tablero[k-1][l+1] == this.jugadorAgente) {
                        row.add(k); col.add(l);  // guardar la fila y columna hacia el cual podria moverse.
                        origRowCol.add(new int[]{i,j}); // guardar la fila y columna desde el cual podria moverse.
                        huboCaptura.add(true);          // guardar indicador sobre si habra captura de ficha del contrario en la posible movida.
                        huboMov = true;                 // indica si ocurrio captura de ficha del contrario.
                        hayPosibleMov = true;
                    }

                    // intentar comer, SurEste
                    if (isInRange(k,m) && tablero[k][m] == 0 && tablero[k-1][m-1] == this.jugadorAgente) {
                        row.add(k); col.add(m);
                        origRowCol.add(new int[]{i,j});
                        huboCaptura.add(true);
                        huboMov = true;
                        hayPosibleMov = true;
                    }

                    k = i-2;
                   
                    // la ficha a moverse es una reina
                    if (this.reinasA.contains(i,j)) {
                        
                        // intentar comer, NorOeste
                        if (isInRange(k,l) && tablero[k][l] == 0 && tablero[k+1][l+1] == this.jugadorAgente) {
                            row.add(k); col.add(l);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(true);
                            huboMov = true;
                            hayPosibleMov = true;                            
                        }
                        
                        // intentar comer, NorEste
                        if (isInRange(k,m) && tablero[k][m] == 0 && tablero[k+1][m-1] == this.jugadorAgente) {
                            row.add(k); col.add(m);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(true);
                            huboMov = true;
                            hayPosibleMov = true;
                        }
                    }

                    /* si no existe ningun movimiento en donde pueda realizarse 
                    captura alguna, evaluar movimiento a casillas diagonales 
                    adyacentes. */
                    if (!huboMov) {

                        k = i+1; l=j-1; m=j+1;

                        // moverse a surOeste
                        if (isInRange(k, l) && tablero[k][l] == 0) {
                            row.add(k); col.add(l);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(false);
                            huboMov = true;
                            hayPosibleMov = true;
                        }

                        // moverse al Sureste
                        if (isInRange(k, m) && tablero[k][m] == 0) {
                            row.add(k); col.add(m);
                            origRowCol.add(new int[]{i,j});
                            huboCaptura.add(false);
                            huboMov = true;
                            hayPosibleMov = true;
                        }
                        
                        // la ficha a moverse es una reina
                        if (!huboMov && this.reinasA.contains(i,j)) {
                            
                            k = i-1;

                            // moverse a NorOeste
                            if (isInRange(k, l) && tablero[k][l] == 0) {
                                row.add(k); col.add(l);
                                origRowCol.add(new int[]{i,j});
                                huboCaptura.add(false);
                                huboMov = true;
                                hayPosibleMov = true;
                            }

                            // moverse al NorEste
                            if (isInRange(k, m) && tablero[k][m] == 0) {
                                row.add(k); col.add(m);
                                origRowCol.add(new int[]{i,j});
                                huboCaptura.add(false);
                                huboMov = true;
                                hayPosibleMov = true;
                            }
                        }
                    }
                }
            }
        }

        // si hubo algun movimiento, sea capturar o moverse a casilla diagonoal adyacente
        if (hayPosibleMov) {
            
            int random = (int) (Math.random() * row.size());            
            // realizar movimiento de la ficha
            swap(origRowCol.get(random)[0], origRowCol.get(random)[1],
                    row.get(random), col.get(random));
            
            if (jugador==this.jugadorAgente) {
                
                boolean isReina = this.reinasA.contains(origRowCol.get(random)[0], origRowCol.get(random)[1]);
                // si la ficha movida se convierte en nueva reina, o es una existente
                if (row.get(random) == 0 || isReina) {
                    // registrar en la lista de reinas, la nueva posicion
                    this.reinasA.add(row.get(random), col.get(random));
                    
                    // si era una reina existente, remueve la vieja posicion.
                    if (isReina) {
                        this.reinasA.remove(origRowCol.get(random)[0], origRowCol.get(random)[1]);
                        System.out.println("Reina agente: "+(row.get(random)+1)+", "+(col.get(random)+1));
                    } else
                        System.out.println("Nueva Reina agente: "+(row.get(random)+1)+", "+(col.get(random)+1));
                }
                
                if (huboCaptura.get(random)) {
                    
                    int iEliminada = origRowCol.get(random)[0]-(origRowCol.get(random)[0]-row.get(random))/2,
                        jEliminada = origRowCol.get(random)[1]-(origRowCol.get(random)[1]-col.get(random))/2;
                    // eliminar ficha capturada
                    this.tablero[iEliminada][jEliminada] = 0;
                    // actualizar contador
                    this.fichasB--;
                    
                    // si elimino a una reina del oponente
                    if (this.reinasB.contains(iEliminada, jEliminada))
                        this.reinasB.remove(iEliminada, jEliminada);
                }
                
                copiarTablero(tablero, lastTablero);
                
            } else {
                
                boolean isReina = this.reinasB.contains(origRowCol.get(random)[0], origRowCol.get(random)[1]);
                // si la ficha movida se convierte en nueva reina, o es una existente
                if (row.get(random) == 7 || isReina) {
                    // registrar en la lista de reinas, la nueva posicion
                    this.reinasB.add(row.get(random), col.get(random));
                    
                    // si era una reina existente, remueve la vieja posicion.
                    if (isReina) {
                        this.reinasB.remove(origRowCol.get(random)[0], origRowCol.get(random)[1]);
                        System.out.println("Reina oponente: "+(row.get(random)+1)+", "+(col.get(random)+1));
                    } else
                        System.out.println("Reina NUEVA oponente: "+(row.get(random)+1)+", "+(col.get(random)+1));
                }
                
                if (huboCaptura.get(random)) {
                    
                    int iEliminada = origRowCol.get(random)[0]-(origRowCol.get(random)[0]-row.get(random))/2,
                        jEliminada = origRowCol.get(random)[1]-(origRowCol.get(random)[1]-col.get(random))/2;
                    // eliminar ficha capturada
                    this.tablero[iEliminada][jEliminada] = 0;
                    // actualizar contador
                    this.fichasA--;
                    
                    // si elimino a una reina del agente
                    if (this.reinasA.contains(iEliminada, jEliminada))
                        this.reinasA.remove(iEliminada, jEliminada);
                }
            }
            
        } else {
            if (jugador == this.jugadorAgente) {
                this.noMovA = true;
                System.out.println("Agente declara que no puede continuar");
            }
            else {
                this.noMovB = true;
                System.out.println("Contrario declara que no puede continuar");
            }
        }
        
        //si es el agente, actualizar ultimo tablero
//        if (jugador == this.jugadorAgente) {
//            copiarTablero(tablero, lastTablero);
//        }
    }

    private void jugarHumano(int jugador) { // siempre es oponente

        printTablero();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String x1 = "", y1="", x2="", y2="";
        
        try {
            x1 = br.readLine();
            y1 = br.readLine();
            x2 = br.readLine();
            y2 = br.readLine();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        int X1 = Integer.parseInt(x1) - 1;
        int Y1 = Integer.parseInt(y1) - 1;
        int X2 = Integer.parseInt(x1) - 1;
        int Y2 = Integer.parseInt(y1) - 1;
        
        // evitar desborde y verificar que la ficha elegida para mover sea la correcta (del enemigo)
        if (isInRange(X1, Y1) && isInRange(X2, Y2) && tablero[X1][Y1] == jugador) {
            
            boolean isReina = this.reinasB.contains(X1,Y1);
            String movimiento = tipoMov(X1,Y1,X2,Y2);
            boolean huboCaptura = false, huboMov = false;
            
            switch (movimiento) {
                case "noroesteC": // C significa que se mueve para comer.
                    if (tablero[X1-1][Y1-1] == this.jugadorAgente && tablero[X2][Y2] == 0 && this.reinasB.contains(X1,Y1)) { // se puede saltar y comer 1 ficha del agente
                        
                        swap(X1, Y1, X2, Y2);
                        this.tablero[X1-1][Y1-1] = 0;
                        this.fichasA--;

                        // si la ficha del agente eliminada es una reina
                        if (this.reinasA.contains(X1-1, Y1-1)) {
                            // remover de la lista su posicion
                            this.reinasA.remove(X1-1, Y1-1);
                        }
                        huboCaptura = true;
                    
                        // si la ficha del jugador(siempre es el enemigo de agente) es una nueva reina o ya era reina
                        if (X2 == 7 || isReina) {
                            this.reinasB.add(X2,Y2);
                            // si ya era una reina, remover su antigua posicion de la lista.
                            if (isReina) this.reinasB.remove(X1,Y1);
                        }
                    }
                    break;
                case "noresteC":
                    if (tablero[X1-1][Y1+1] == this.jugadorAgente && tablero[X2][Y2] == 0 && this.reinasB.contains(X1,Y1)) { // se puede saltar y comer 1 ficha del agente
                        
                        swap(X1, Y1, X2, Y2);
                        this.tablero[X1-1][Y1+1] = 0;
                        this.fichasA--;
                        
                        if (this.reinasA.contains(X1-1, Y1+1)) {
                            this.reinasA.remove(X1-1, Y1+1);
                        }
                        huboCaptura = true;
                        
                        if (X2 == 7 || isReina) {
                            this.reinasB.add(X2,Y2);
                            if (isReina) this.reinasB.remove(X1,Y1);
                        }
                    }
                    break;
                case "suroesteC":
                    if (tablero[X1+1][Y1-1] == this.jugadorAgente && tablero[X2][Y2] == 0 && this.reinasB.contains(X1,Y1)) { // se puede saltar y comer 1 ficha del agente
                        
                        swap(X1, Y1, X2, Y2);
                        this.tablero[X1+1][Y1-1] = 0;
                        this.fichasA--;
                        
                        if (this.reinasA.contains(X1+1, Y1-1)) {
                            this.reinasA.remove(X1+1, Y1-1);
                        }
                        huboCaptura = true;
                        
                        if (X2 == 7 || isReina) {
                            this.reinasB.add(X2,Y2);
                            if (isReina) this.reinasB.remove(X1,Y1);
                        }
                    }
                    break;
                case "suresteC":
                    if (tablero[X1+1][Y1+1] == this.jugadorAgente && tablero[X2][Y2] == 0 && this.reinasB.contains(X1,Y1)) { // se puede saltar y comer 1 ficha del agente
                        
                        swap(X1, Y1, X2, Y2);
                        this.tablero[X1+1][Y1+1] = 0;
                        this.fichasA--;
                        
                        if (this.reinasA.contains(X1+1, Y1+1)) {
                            this.reinasA.remove(X1+1, Y1+1);
                        }
                        huboCaptura = true;
                        
                        if (X2 == 7 || isReina) {
                            this.reinasB.add(X2,Y2);
                            if (isReina) this.reinasB.remove(X1,Y1);
                        }
                    }
                    break;
            }
            
            huboMov = huboCaptura;
            
            if (!huboCaptura && movimiento.compareTo("noval") != 0) { // es un movimiento en casilla diagonal adyacente
                
                if(tablero[X2][Y2] == 0) { // si la casilla esta libre
                
                    swap(X1, Y1, X2, Y2);
                    huboMov = true;

                    if (X2 == 7 || isReina) {
                        this.reinasB.add(X2,Y2);
                        if (isReina) this.reinasB.remove(X1,Y1);
                    }
                }
            }
            
            if (!huboMov) this.noMovB = true;
        }
//        int xArray[] = {2, 2, 2, 1, 1, 1, 0, 0, 0};
//        int yArray[] = {0, 1, 2, 0, 1, 2, 0, 1, 2};
//        int x = xArray[posicion];
//        int y = yArray[posicion];

        //aplicar jugada
//        tablero[x][y] = jugador;
    }
    
    private String tipoMov(int x1, int y1, int x2, int y2) {
        return (x1-x2==2 && y1-y2==2) ? "noroesteC" : // C indica que es un posible movimiento para capturar fichas
                ( (x1-x2==2 && y1-y2==-2) ? "noresteC" :
                ( (x1-x2==-2 && y1-y2==2) ? "suroesteC" :
                ( (x1-x2==-2 && y1-y2==-2) ? "suresteC" :
                ( (x1-x2==1 && y1-y2==1? "noroeste" :
                ( (x1-x2==1 && y1-y2==-1)? "noreste" :
                ( (x1-x2==-1 && y1-y2==1)? "suroeste" :
                ( (x1-x2==-1 && y1-y2==-1)? "sureste" : "noval"))))))));
    }

    public void printTable() {

        for (String key : lookupTable.keySet()) {

            System.out.println("Tablero: " + key + ", prob: " + lookupTable.get(key));
            printTablero(deserializar(key));
        }
    }

    public void printTablero() {

        printTablero(this.tablero);
    }

    public void printTablero(int[][] tablero) {

        System.out.println();
//        System.out.print("---------------------");
        System.out.print("   1 2 3 4 5 6 7 8");
        System.out.println();
        for (int i = 0; i < tablero.length; i++) {
            System.out.print((i+1) + " ");
            for (int j = 0; j < tablero[0].length; j++) {

                System.out.print("|");
                if (tablero[i][j] == 1) {
                    System.out.print("x");
                } else if (tablero[i][j] == 2) {
                    System.out.print("o");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.print("|");
//            System.out.println();
//            System.out.print("---------------------");
            System.out.println();
        }
    }

    private void copiarTablero(int[][] tableroOrigen, int[][] tableroDestino) {

        for (int i = 0; i < tableroOrigen.length; i++) {
            for (int j = 0; j < tableroOrigen[0].length; j++) {

                tableroDestino[i][j] = tableroOrigen[i][j];
            }
        }
    }

    public int getN() {

        return this.N;
    }

    public void setN(int N) {

        this.N = N;
    }

    public double getAlpha() {

        return this.alpha;
    }

    public void setAlpha(double alpha) {

        this.alpha = alpha;
    }

    public void updateAlpha(int currentGame) {

        this.alpha = 0.5 - 0.49 * currentGame / this.N;
    }

    public int getResult() {

        return this.gameResult;
    }

    public void jugarVsRandom() {

        int jugador = this.jugadorAgente;
        int contrario = (jugador % 2) + 1;
        int turno = 1;
        //int jugadas = 9;
        double q;
        do {
            this.noMovA = false;
            this.noMovB = false;
            
            if (turno == jugador) {
                q = Math.random();
//                q = 0.6;
                if (q <= qRate || !this.entrenar) {
                    System.out.println("Agente juega por jugar()");
                    jugar(jugador);
                } else {
                    System.out.println("Agente juega por jugarRandom()");
                    jugarRandom(jugador);
                }
            } else {
                jugarRandom(contrario);
            }
            
            //actualizar resultado
            gameResult = calculateResult(tablero);
            
            System.out.println("gameresult: "+gameResult);
            System.out.println("noMovA: "+noMovA);
            System.out.println("noMovB: "+noMovB);
            
            if (gameResult > 0) { //ya hay resultado
                if (gameResult != jugador && entrenar) //perdimos o empatamos, actualizar tablero
                {
                    updateProbability(lastTablero, calculateReward(tablero, jugador), jugador);
                }
                break;
            } // si es cero, continua el juego.

            turno = 2 - turno + 1;
            //jugadas--;
            
            this.printTablero();
            System.out.println();
        } while ((!this.noMovA && !this.noMovB) && (this.fichasA>0 && this.fichasB > 0));

    }

    public void setQRate(double qRate) {

        this.qRate = qRate;
    }

    public void jugarVsHumano() {

        int jugador = this.jugadorAgente;
        int contrario = (jugador % 2) + 1;
        int turno = 1;
//        int jugadas = 9;
        do {
            
            this.noMovA = false;
            this.noMovB = false;
            
            if (turno == jugador) {
                jugar(jugador);
            } else {
                jugarHumano(contrario);
            }

            //actualizar resultado
            gameResult = calculateResult(tablero);
            if (gameResult > 0) { //ya hay resultado
                if (gameResult != jugador && entrenar) //perdimos, actualizar tablero
                {
                    updateProbability(lastTablero, calculateReward(tablero, jugador), jugador);
                }
                break;
            }

            turno = 2 - turno + 1;
//            jugadas--;
        } while ((!this.noMovA && !this.noMovB) && (this.fichasA>0 && this.fichasB > 0));

    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        int trainingCount = 100;
        int humanTrainingCount = 1;
        double totalGamesCount = 50;
        int totalExperiments = 1;
        //Double qRates[] = {0.1, 0.2, 0.3, 0.4, 0.5};
        Double qRates[] = {0.5};
        for (int q = 0; q < qRates.length; q++) {

            double winsRatioAcum = 0;
            double lossesRatioAcum = 0;
            double drawsRatioAcum = 0;
            for (int k = 0; k < totalExperiments; k++) {

                DamasInglesas ag = new DamasInglesas(trainingCount);
                ag.setQRate(qRates[q]);
                System.out.println("Jugar vs Random - Fase de entrenamiento");
                for (int i = 0; i < ag.getN(); i++) {
                    System.out.println("Entrenamiento Nº: "+i);
                    ag.reset(true);
                    ag.updateAlpha(i);
                    ag.jugarVsRandom();
                }

//                ag.setN(humanTrainingCount);
//                ag.setAlpha(0.7);
//                System.out.print("Jugar vs Humano");
//                for (int i = 0; i < ag.getN(); i++) {
//                    ag.reset(true);
//                    //ag.updateAlpha(i);
//                    ag.jugarVsHumano();
//                }

                System.out.println(">>>>>>>>>>>>>>> AFTER TRAINING ");
                ag.printTable();

                int wins = 0;
                int losses = 0;
                int draws = 0;
                int contrario = 2 - ag.jugadorAgente + 1;
                for (int i = 0; i < totalGamesCount; i++) {
                    System.out.println("Juego Nº: "+i);
                    ag.reset(false);
                    ag.jugarVsRandom();

                    if (ag.getResult() == ag.jugadorAgente) {
                        wins++;
                    } else if (ag.getResult() == contrario) {
                        losses++;
                    } else {
                        draws++;
                    }
                }

                /*
                    System.out.println("Wins: " + wins + ", Losses: " + losses + ", Draws: " + draws);
                    System.out.println("Ratio W/T: " + wins/totalGamesCount);
                    System.out.println("Ratio L/T: " + losses/totalGamesCount);
                    System.out.println("Ratio D/T: " + draws/totalGamesCount);
                 */
                winsRatioAcum += wins / totalGamesCount;
                lossesRatioAcum += losses / totalGamesCount;
                drawsRatioAcum += draws / totalGamesCount;
            }
            System.out.println(">>>>>>>>>>>>>>> RATIO AVG, Q RATE: " + qRates[q]);
            System.out.println("Ratio Avg W/T: " + winsRatioAcum / totalExperiments);
            System.out.println("Ratio Avg L/T: " + lossesRatioAcum / totalExperiments);
            System.out.println("Ratio Avg D/T: " + drawsRatioAcum / totalExperiments);
        }
    }
}