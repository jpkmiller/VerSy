package common.msgtypes;

import common.FishModel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class CollectToken implements Serializable {
    private final Set<FishModel> globalState;

    public CollectToken () {
        globalState = new HashSet<>();
    }

    public void addGlobalState(Set<FishModel> fish) {
        globalState.addAll(fish);
    }

    public Set<FishModel> getGlobalState() { return globalState; }
}
