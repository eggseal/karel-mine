package main;
import java.awt.Color;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import kareltherobot.Directions;
import kareltherobot.Robot;

public class MejorRobot extends Robot {
    public static final Semaphore moveMutex = new Semaphore(1); // Solo un robot se mueve a la vez
    public static final Vector<MejorRobot> robots = new Vector<>(); // Vector con todos los robots del mundo

    public int street; // La calle en que está el robot
    public int avenue; // La avenida en que está el robot
    public int beepers; // La cantidad de beepers que lleva
    public static int max = 0; // El maximo de beepers que puede cargar
    public boolean dead; // Si ya se murió

    public Semaphore posMutex = new Semaphore(0); // Bloqueo a otros robots que se quieran mover encima de este robot
    public boolean waiting = false; // Si hay otros robots esperando a que este se mueva

    // Traduccion de los bits de direccion a un objeto de direccion
    public static final Direction[] DIRECTIONS = { Directions.West, Directions.South, Directions.East, Directions.North };
    public int direction; // Direccion actual en 2 bits

    public static Object exitOrder = new Object(); // Objeto usado para la notificacion de salida
    public static boolean exit = false; // Si ya se hizo la orden de salida

    Thread thread; // El hilo de este robot en caso de necesitarse

    public MejorRobot(int street, int avenue, Color color) {
        super(street, avenue, Directions.North, 0, color);
        this.street = street;
        this.avenue = avenue;
        this.beepers = 0; // Empieza sin beepers
        this.direction = 3; // Empieza mirando al norte

        robots.add(this);

        this.thread = new Thread(this);
        this.thread.start();
    }

    public static MejorRobot checkRobot(MejorRobot you, int street, int avenue) {
        // Enumera a todos los robots
        Enumeration<MejorRobot> r = robots.elements();
        MejorRobot o;
        do {
            // Ninguno está en esta posicion
            if (!r.hasMoreElements()) return null;
            // Pasa a probar con el siguiente
            o = r.nextElement();
        } while (o == you || !(street == o.street && avenue == o.avenue)); 
        // Repite hasta que haya un robot en esa posicion y no sea el que está buscando 

        return o;
    }

    @Override
    public void turnLeft() {
        // Aumenta la direccion regresandola si se pasa de 3
        direction = (direction + 1) & 0b11;
        super.turnLeft();
    }

    public void turn(Direction dir) {
        while (DIRECTIONS[direction] != dir) turnLeft();
    }

    @Override 
    public void turnOff() {
        // Suelta a cualquier robot esperandolo
        posMutex.release();

        // Se muere
        dead = true;
        super.turnOff();
    }

    @Override
    public void move() {
        boolean moveVer = (direction & 1) == 1; // Si va a cambiar su calle
        boolean increase = (direction >> 1 & 1) == 1; // Si la direccion va a aumentar

        int i = increase ? 1 : -1; // El valor del incremento (decremento negativo)

        // Define la nueva posicion si se mueve
        int newStreet = street;
        int newAvenue = avenue;
        if (moveVer) newStreet += i;
        else newAvenue += i;

        try {
            // Espera a ser el unico moviendose
            moveMutex.acquire();
            // Agarra al robot que se encuentre en la nueva posicion
            MejorRobot occupied = checkRobot(this, newStreet, newAvenue);

            if (occupied != null) {
                // Si el que está ocupando está muerto, tambien se muere
                if (occupied.dead) {
                    moveMutex.release(); // Suelta el semaforo de movimiento antes de
                    turnOff();
                    return;
                }
                // Si no está muerto espera a que se quite
                occupied.waiting = true;
                moveMutex.release();
                occupied.posMutex.acquire();
                return; // Vuelve a hacer el checkeo cuando el otro robot se quite
            }

            // Se pasa a la nueva posicion
            street = newStreet;
            avenue = newAvenue;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Si habia otro robot esperando a este, suelta el semaforo
        if (waiting) posMutex.release();

        // Si se movió donde habia alguien (no deberia) se mata
        MejorRobot occupied = checkRobot(this, newStreet, newAvenue);
        if (occupied != null) {
            occupied.turnOff();
            turnOff();
        }

        moveMutex.release();
        super.move();
    }

    public void moveTo(int pos) {
        // Se mueve hasta que la calle o avenida (dado por la direccion) sean su destino
        boolean moveVer = (direction & 1) == 1;
        while (((moveVer && street != pos) || (!moveVer && avenue != pos)) && !dead) {
            move();
        }
    }

    // Se mueve a (1, 1)
    public void moveToEntrance() {
        if (avenue > 2) {
            turn(West);
            moveTo(2);
        } else if (avenue < 2) {
            turn(East);
            moveTo(2);
        } 
        turn(South);
        moveTo(7);
        turn(West);
        moveTo(1);
        turn(South);
        moveTo(1);
    }

    // Sigue el recorrido desde extraccion hacia (12, 11)
    public void moveToMine() {
        turn(East);
        moveTo(8);
        turn(North);
        moveTo(11);
        turn(East);
        moveTo(12);
    }

    // Sigue el recorrido desde la mina hacia (3, 2)
    public void moveToExtraction() {
        turn(South);
        moveTo(6);
        turn(West);
        moveTo(3);
        turn(South);
        moveTo(2);
    }

    public void moveToWait(boolean skip) {
        if (!skip) {
            turn(South);
            moveTo(10);
        }
        turn(East);
        moveTo(14);
    }

    public void moveToBodegas() {
        turn(West);
        moveTo(1);
        turn(North);
        moveTo(7);
        turn(East);
        move();
        turn(North);
        move();
        turn(East);
        move();
    }

    public void exitFromMine(Direction initial, int limitS, int limitA) {
        turn(initial);
        moveTo(13);
        turn(South);
        moveTo(6);
        turn(West);
        moveTo(3);
        turn(South);
        moveTo(1);
        turn(West);
        moveTo(1);
        turn(North);
        moveTo(7);
        turn(East);
        moveTo(2);
        turn(North);
        moveTo(limitS);
        turn(East);
        moveTo(limitA);
    }

    public void exitFromExtract(int limitS, int limitA) {
        if (street < 7) {
            turn(West);
            moveTo(1);
            turn(North);
            moveTo(7);
            turn(East);
            moveTo(2);
        }
        turn(North);
        moveTo(limitS);
        turn(East);
        moveTo(limitA);
    }
}