package com.dcr.datamodels;

public class RelationCouple {
    public Activity Activity1;
    public Activity Activity2;

    public RelationCouple(Activity a1, Activity a2)
    {
        Activity1 = new Activity(a1);
        Activity2 = new Activity(a2);
    }

    @Override
    public boolean equals(Object obj) {
        RelationCouple otherCouple = (RelationCouple) obj;
        // Either (1 == 1 && 2 == 2) or (1 == 2 and 2 == 1)
        return otherCouple != null &&
                ((Activity1.getId().equals(otherCouple.Activity1.getId())
                        && Activity2.getId().equals(otherCouple.Activity2.getId()))
                        || (Activity1.getId().equals(otherCouple.Activity2.getId())
                            && Activity2.getId().equals(otherCouple.Activity1.getId())));
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 23 + Activity1.getId().hashCode() + Activity2.getId().hashCode();
        return hash;
    }
}
