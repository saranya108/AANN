package com;

import java.util.ArrayList;
import java.util.Collections;

public class Path {
	public ArrayList<NPoint> path = new ArrayList<NPoint>();
	public NPoint start = null;
	public NPoint end = null;
	public double pathdist = 0;

	public Path(NPoint start, NPoint end, ArrayList<NPoint> path, double pathdist){
		this.start = start;
		this.end = end;
		this.path = path;
		this.pathdist = pathdist;
	}

	public ArrayList<NPoint> reversePath(){
		ArrayList<NPoint> revPath = new ArrayList<NPoint>();
		revPath.addAll(path);
		Collections.reverse(revPath);
		return revPath;
	}

	@Override
	public String toString() {
		return "Path [path=" + path + ", start=" + start + ", end=" + end
				+ ", pathdist=" + pathdist + "]";
	}
}
