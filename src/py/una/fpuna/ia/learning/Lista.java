/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.una.fpuna.ia.learning;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author daniel
 */
public class Lista {
    private ArrayList<String> element;
    
    public Lista() {
        this.element = new ArrayList<>();
    }
    
    public void add(int i, int j) {
        String s;
        s = String.valueOf(i).concat(String.valueOf(j));
        this.element.add(s);
    }
    
    public boolean remove(int i, int j) {
        String s;
        s = String.valueOf(i).concat(String.valueOf(j));
        return this.element.remove(s);
    }
    
    public boolean contains(int i, int j) {
        String s;
        s = String.valueOf(i).concat(String.valueOf(j));
        return this.element.contains(s);
    }
    
    public static void main(String[] args) {
        Lista lista = new Lista();
        lista.add(1, 2);
        System.out.println(lista.contains(1, 3));
    }
}
