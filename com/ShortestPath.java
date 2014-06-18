package com;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;

public class ShortestPath {
	public static ArrayList<NPoint> getSPOnRawNetwork(NPoint target, NPoint source, Set<Integer> toBeChanged){
		source.minDist = 0;
		source.f = computeDistance(source, target);
		toBeChanged.add(source.getId());
		PriorityQueue<NPoint> npQueue = new PriorityQueue<NPoint>();
		Set <NPoint> processed =  new HashSet<NPoint>();
		NPoint u = null;
		npQueue.add(source);
		while (!npQueue.isEmpty()){
			u = npQueue.poll();
			processed.add(u);
			if(u == target)
				break;
			for (NPoint v : ProcessData.getRnet(u)){
				if(!processed.contains(v)){
					double weight = computeDistance(u,v);
					double distanceThroughU = u.minDist + weight;
					if (Double.compare(distanceThroughU, v.minDist) < 0) {
						npQueue.remove(v);
						v.minDist = distanceThroughU;
						if(v.f < 0)
							v.f = computeDistance(v, target);
						toBeChanged.add(v.getId());
						v.previous = u;
						npQueue.add(v);
					}
				}
			}
		}
		if(npQueue.isEmpty() && u != target){
			System.out.println("HERE");
		}
		ArrayList<NPoint> path = new ArrayList<NPoint>();
		for (NPoint p = target; p != null; p = p.previous){
			path.add(p);
		}
		Collections.reverse(path);
		return path;
	}

	public static ArrayList<NPoint> getSPOnRawNetworkToTargetList(Set<NPoint> targetList, NPoint source, Set<Integer> toBeChanged, NPoint dest){
		source.minDist = 0;
		source.f =  computeDistance(source, dest);
		toBeChanged.add(source.getId());
		PriorityQueue<NPoint> npQueue = new PriorityQueue<NPoint>();
		Set <NPoint> processed =  new HashSet<NPoint>();
		NPoint u = null;
		npQueue.add(source);
		while (!npQueue.isEmpty()){
			u = npQueue.poll();
			processed.add(u);
			if(targetList.contains(u))
				break;
			for (NPoint v : ProcessData.getRnet(u)){
				if(!processed.contains(v)){
					double weight = computeDistance(u,v);
					double distanceThroughU = u.minDist + weight;
					if (Double.compare(distanceThroughU, v.minDist) < 0) {
						npQueue.remove(v);
						v.minDist = distanceThroughU;
						if(v.f < 0) {
							v.f =  computeDistance(v, dest);
						}
						toBeChanged.add(v.getId());
						v.previous = u;
						npQueue.add(v);
					}
				}
			}
		}
		if(npQueue.isEmpty() && !targetList.contains(u)){
			System.out.println("HERE");
		}
		ArrayList<NPoint> path = new ArrayList<NPoint>();
		for (NPoint p = u; p != null; p = p.previous){
			path.add(p);
		}
		Collections.reverse(path);
		return path;
	}

	public static ArrayList<NPoint> getSPOnGrid(Set<NPoint> targetRPList, NPoint sourceRP, Set<Integer> toBeChanged, NPoint dest){
		sourceRP.minDist = 0;
		sourceRP.f =  computeDistance(sourceRP, dest);
		
		Set <NPoint> processed =  new HashSet<NPoint>();
		toBeChanged.add(sourceRP.getId());
		NPoint u = null;
		PriorityQueue<NPoint> npQueue = new PriorityQueue<NPoint>();
		npQueue.add(sourceRP);
		while (!npQueue.isEmpty()){
			u = npQueue.poll();
			processed.add(u);
			if(targetRPList.contains(u))
				break;
			List<Path> spaths = Grid.gnet.get(u);
			if(spaths == null){
				continue;
			}
			Iterator <Path> itr = spaths.iterator();
			while(itr.hasNext()){
				Path p = itr.next();
				NPoint v = p.end;
				if(!processed.contains(v)){
					double weight = p.pathdist;
					double distanceThroughU = u.minDist + weight;
					if (Double.compare(distanceThroughU, v.minDist) < 0) {
						npQueue.remove(v);
						v.minDist = distanceThroughU;
						if(v.f < 0) {
							v.f =  computeDistance(v, dest);
						}
						toBeChanged.add(v.getId());
						v.previous = u;
						npQueue.add(v);
					}
				}
			}
		}

		if(npQueue.isEmpty() && !targetRPList.contains(u)){
			System.out.println("HERE RP");
		}
		ArrayList<NPoint> path = new ArrayList<NPoint>();
		for (NPoint p = u; p != null; p = p.previous){
			path.add(p);
		}
		Collections.reverse(path);
		return path;
	}

	public static double computeDistance(NPoint start, NPoint end) {
		double xd = start.getX()-end.getX();
		double yd = start.getY()-end.getY();
		double res = Math.sqrt(xd*xd + yd*yd);
		return res;
	}
}
