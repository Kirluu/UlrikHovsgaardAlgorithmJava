package com.dcr.datamodels;

public class Relation {

    private RelationType Type;
    public RelationType getType() { return Type; }

    private Activity Source, Target;
    public Activity getSource() { return Source; }
    public Activity getTarget() { return Target; }

    public Relation(RelationType t, Activity s, Activity tar)
    {
        Type = t;
        Source = s;
        Target = tar;
    }

    @Override
    public String toString()
    {
        return String.format("%s from %s to %s", Type, Source.getId(), Target.getId());
    }
}
