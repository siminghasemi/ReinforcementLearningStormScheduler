package SiminRLStormScheduler.scheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ReplayBuffer {
    private final List<Experience> buffer;
    private final int capacity;

    public ReplayBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayList<>();
    }

    public void add(Experience exp) {
        if (buffer.size() >= capacity) {
            buffer.remove(0);
        }
        buffer.add(exp);
    }

    public List<Experience> sample(int batchSize) {
        List<Experience> sample = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < batchSize; i++) {
            sample.add(buffer.get(rand.nextInt(buffer.size())));
        }
        return sample;
    }


    public int size() {
        return buffer.size();
    }
}
