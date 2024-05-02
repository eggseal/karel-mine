package main;
import java.util.concurrent.Semaphore;

public class Extraccion {
    public final Semaphore beeperMutex = new Semaphore(1); // Solo 1 robot interactua con la cantidad de beepers a la vez
    public final Semaphore puntoMutex = new Semaphore(1); // Solo 1 robot está parado en el punto de extraccion a la vez
    
    public int beepers; // Cantidad de beepers en el punto

    public Extraccion() {
        // Todos los puntos empiezan vacios
        beepers = 0;
    }

    public int getBeepers() {
        try {
            // Espera a ser el unico interactuando con los beepers y saca su valor
            beeperMutex.acquire();
            return beepers;
        } catch (InterruptedException e) {
            // Imprime el error y retorna -1 indicando error
            e.printStackTrace();
            return -1;
        } finally {
            // Siempre suelta el semaforo al terminar
            beeperMutex.release();
        }
    }

    public void putBeeper(MejorRobot robot) {
        try {
            // Espera a ser el unico interactuando con los beepers
            beeperMutex.acquire();
            // Aumenta los beepers del punto quitandole un beeper al robot
            beepers++;
            robot.beepers--;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Siempre suelta el semaforo al terminar
            beeperMutex.release();
        }
        // Ejecuta la accion de poner el beeper
        robot.putBeeper();
    }

    public void pickBeeper(MejorRobot robot) {
        try {
            // Espera a ser el unico interactuando con los beepers
            beeperMutex.acquire();
            // Reduce los beepers del punto dandole un beeper al robot
            beepers--;
            robot.beepers++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Siempre suelta el semaforo al terminar
            beeperMutex.release();
        }
        // Ejecuta la accion de quitar el beeper
        robot.pickBeeper();
    }
}
