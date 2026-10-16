import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    static final int FINISH = 100;
    static final AtomicBoolean finished = new AtomicBoolean(false);
    static final AtomicReference<String> winner = new AtomicReference<>();


    static class Runner implements Runnable {
        private final String title;
        private final Random rnd = new Random();
        private volatile int position = 0;
        private Thread thread;

        Runner(String title) {
            this.title = title;
        }

        void attach(Thread t) {
            this.thread = t;
        }

        int getPosition() {
            return position;
        }

        String getTitle() {
            return title;
        }

        @Override
        public void run() {
            while (position < FINISH && !finished.get()) {
                int p = thread.getPriority();

                // Чем выше приоритет, тем длиннее возможный шаг
                int step = 1 + rnd.nextInt(p + 1);
                position = Math.min(FINISH, position + step);
                System.out.printf("%-9s | %3d м | приоритет %2d%n", title, position, p);

                // compareAndSet гарантирует, что победитель будет ровно один
                if (position >= FINISH && finished.compareAndSet(false, true)) {
                    winner.set(title);
                    System.out.println(">>> " + title + " пересекает финиш!");
                }

                Thread.yield(); //
                try {
                    Thread.sleep(120 - p * 10); // от 110 мс (приоритет 1) до 20 мс (приоритет 10)
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    // Судья повышает приоритет отстающему
    static void boostLaggard(Runner a, Thread ta, Runner b, Thread tb) {
        if (!ta.isAlive() || !tb.isAlive()) return;

        Runner lagging = a.getPosition() < b.getPosition() ? a : b;
        Thread laggingThread = (lagging == a) ? ta : tb;
        if (a.getPosition() == b.getPosition()) return;

        int p = laggingThread.getPriority();
        if (p < Thread.MAX_PRIORITY) {
            laggingThread.setPriority(p + 1);
            System.out.println("[судья] " + lagging.getTitle() + " отстаёт, приоритет -> " + (p + 1));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Кролик против черепахи ===");

        Runner rabbit = new Runner("Кролик");
        Runner turtle = new Runner("Черепаха");

        Thread rabbitThread = new Thread(rabbit, "rabbit");
        Thread turtleThread = new Thread(turtle, "turtle");
        rabbit.attach(rabbitThread);
        turtle.attach(turtleThread);

        rabbitThread.setPriority(Thread.MAX_PRIORITY);
        turtleThread.setPriority(Thread.MIN_PRIORITY);
        System.out.println("Старт: кролик " + rabbitThread.getPriority()
                + ", черепаха " + turtleThread.getPriority() + "\n");

        rabbitThread.start();
        turtleThread.start();

        while (rabbitThread.isAlive() || turtleThread.isAlive()) {
            Thread.sleep(400);
            boostLaggard(rabbit, rabbitThread, turtle, turtleThread);
            System.out.println("--- кролик " + rabbit.getPosition()
                    + " м, черепаха " + turtle.getPosition() + " м ---");
        }

        rabbitThread.join();
        turtleThread.join();

        System.out.println("\nПобедитель: " + winner.get());
        System.out.println("Кролик: " + rabbit.getPosition() + " м, черепаха: " + turtle.getPosition() + " м");
    }
}