package com;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygons2D;

public class ProcessData {
	private static double minx = Double.POSITIVE_INFINITY;
	private static double miny = Double.POSITIVE_INFINITY;
	private static double maxx = Double.NEGATIVE_INFINITY;
	private static double maxy = Double.NEGATIVE_INFINITY;
	static Map<NPoint,List<NPoint>> rnet = new HashMap<NPoint,List<NPoint>>();
	private static ArrayList<DPoint> dpoints = new ArrayList<DPoint>();
	private static Polygon2D queryarea = null;
	private static Map<Integer,NPoint> npId = new HashMap<Integer, NPoint>();
	private static PrintWriter writer = null;

	public static List<NPoint> getRnet(NPoint p) {
		return rnet.get(p);
	}
	public static Set<NPoint> getRnetNodes() {
		return rnet.keySet();
	}
	public static ArrayList<DPoint> getDpoints() {
		return dpoints;
	}

	public static void main(String[] args){
		try {
			BufferedReader reader = Files.newBufferedReader(Paths.get("config.txt"), Charset.defaultCharset());
			String line;
			while((line = reader.readLine())!=null){
				processFile(Paths.get("OL_raw_node.txt"),"nn");
				processFile(Paths.get("OL_raw_edges.txt"),"ne");
				processFile(Paths.get("OL_data_points.txt"),"dn");
				long startTime = System.nanoTime();
				String[] attr  = line.split(",");
				String rowscol = attr[0];
				String fname = attr[1];
				System.out.println(fname + " Done file reading");
				Grid.buildGrid(minx,maxx,miny,maxy, rowscol);
			//	Grid.distributeDPointsRandom();
				Grid.distributeDPoints(dpoints);
				Grid.distributeNPoints(rnet.keySet());
				Grid.findRPShortestPath();
				long stopTime = System.nanoTime();
				double precompTime = (stopTime - startTime)/1e9;
				try {
					writer = new PrintWriter(fname);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				writer.write(precompTime+"\n");
				/*writer.write("My Algo Time,Std Algo Time,My Q1 Pathdist,Std Q1 Pathdist,My Q2 Pathdist,"
						+ "Std Q1 Pathdist,My Q3 Pathdist,Std Q3 Pathdist,Time diff,Path diff,Normalized Path diff\n");
				System.out.print("Running queries");
				for(int i=0;i<1000;i++){
					System.out.print(".");
					generateQueries();
				}*/
				System.out.print("\n");
				rnet.clear();
				minx = Double.POSITIVE_INFINITY;
				miny = Double.POSITIVE_INFINITY;
				maxx = Double.NEGATIVE_INFINITY;
				maxy = Double.NEGATIVE_INFINITY;
				dpoints.clear();
				queryarea = null;
				npId.clear();

				Grid.gnet.clear();
				writer.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	public static void generateQueries(){
		Random random = new Random();
		List<NPoint> keys = new ArrayList<NPoint>(rnet.keySet());
		NPoint q1 = keys.get(random.nextInt(keys.size()));
		NPoint q2 = keys.get(random.nextInt(keys.size()));
		NPoint q3 = keys.get(random.nextInt(keys.size()));
		Set<Integer> toBeChanged = new HashSet<>();

		Point2D iptemp = getIntermediatePoint(q1,q2,q3);
		Point ip = new Point(iptemp.getX(),iptemp.getY());

		long startTime = System.nanoTime();
		Point dpSelected = null;
		dpSelected = Grid.getDataPointForQuery(ip);
		GCell igc = Grid.getGridCell(ip.getX(),ip.getY());
		NPoint NNdpSelected = Grid.calculateNNP(dpSelected, igc.getNpl());
		try{
			GCell gc = Grid.getGridCell(NNdpSelected.getX(),NNdpSelected.getY());
		}catch(Exception e){
			return;
		}
		com.Path spath1 = getANNPath(q1, NNdpSelected);
		com.Path spath2 = getANNPath(q2, NNdpSelected);
		com.Path spath3 = getANNPath(q3, NNdpSelected);
		long stopTime = System.nanoTime();
		double mytime = (stopTime - startTime)/1e9;

		//System.out.println("Time taken = " + mytime);
		startTime = System.nanoTime();
		ArrayList <NPoint> temp = ShortestPath.getSPOnRawNetwork(NNdpSelected, q1, toBeChanged);
		com.Path spath4 = new com.Path(NNdpSelected, q1, temp, NNdpSelected.minDist);
		for(int i : toBeChanged){
			NPoint n = ProcessData.getById(i);
			n.previous = null;
			n.minDist =  Double.POSITIVE_INFINITY;
			n.f =  -1;
		}
		toBeChanged.clear();

		temp = ShortestPath.getSPOnRawNetwork(NNdpSelected, q2, toBeChanged);
		com.Path spath5 = new com.Path(NNdpSelected, q2, temp, NNdpSelected.minDist);
		for(int i : toBeChanged){
			NPoint n = ProcessData.getById(i);
			n.previous = null;
			n.minDist =  Double.POSITIVE_INFINITY;
			n.f =  -1;
		}
		toBeChanged.clear();

		temp = ShortestPath.getSPOnRawNetwork(NNdpSelected, q3, toBeChanged);
		com.Path spath6 =  new com.Path(NNdpSelected, q3, temp, NNdpSelected.minDist);
		for(int i : toBeChanged){
			NPoint n = ProcessData.getById(i);
			n.previous = null;
			n.minDist =  Double.POSITIVE_INFINITY;
			n.f =  -1;
		}
		stopTime = System.nanoTime();
		double stdtime = (stopTime - startTime)/1e9;


		double myNetPathDist = spath1.pathdist + spath2.pathdist + spath3.pathdist;
		double stdPathDist = spath4.pathdist + spath5.pathdist + spath6.pathdist;
		writer.write( mytime + ",");
		writer.write(stdtime + "," + spath1.pathdist +","+ spath4.pathdist +","+ spath2.pathdist);
		writer.write("," + spath5.pathdist + "," + spath3.pathdist + "," + spath6.pathdist);
		writer.write("," + (stdtime-mytime) + "," + (myNetPathDist-stdPathDist) + "," + ((myNetPathDist-stdPathDist)/stdPathDist));
		/*		System.out.println("Time taken for standard = " + ((stopTime - startTime)/1e9));
		System.out.println("sara min dist = " + spath1.pathdist + "," + spath2.pathdist + "," + spath3.pathdist);
		System.out.println("std min dist = " + spath4.pathdist + "," + spath5.pathdist + "," + spath6.pathdist + "\n\n");
		*/
		if(Double.compare(spath1.pathdist,spath4.pathdist) < 0 ||
				Double.compare(spath2.pathdist,spath5.pathdist) < 0 ||
				Double.compare(spath3.pathdist,spath6.pathdist) < 0) {
			writer.write(",Path dist of AANN less than ANN");
		}
		writer.write("\n");
	}

	public static com.Path getANNPath(NPoint q, NPoint NNdpSelected){
		ArrayList <NPoint> plist1 = new ArrayList<>();
		ArrayList <NPoint> plist2 = new ArrayList<>();
		ArrayList <NPoint> plist3 = new ArrayList<>();
		Set<Integer> toBeChanged = new HashSet<>();
		Set<NPoint> tlist = new HashSet<>();

		double dist = 0;
		GCell sgc = Grid.getGridCell(q.getX(),q.getY());

		GCell tgc = Grid.getGridCell(NNdpSelected.getX(),NNdpSelected.getY());

		if(sgc.equals(tgc)){
			plist1 = ShortestPath.getSPOnRawNetwork(NNdpSelected, q, toBeChanged);
			if (plist1.isEmpty())
				return null;
			dist = NNdpSelected.minDist;
			for(int i : toBeChanged){
				NPoint n = ProcessData.getById(i);
				n.previous = null;
				n.minDist =  Double.POSITIVE_INFINITY;
				n.f = -1;
			}
		}else{
			for(int i=0;i<sgc.getRpl().length;i++)
				tlist.add(sgc.getRpl()[i]);
			plist1 = ShortestPath.getSPOnRawNetworkToTargetList(tlist, q, toBeChanged, NNdpSelected);
			if (plist1.isEmpty())
				return null;
			NPoint qRef = plist1.get(plist1.size()-1);
			dist = qRef.minDist;
			for(int i : toBeChanged){
				NPoint n = ProcessData.getById(i);
				n.previous = null;
				n.minDist =  Double.POSITIVE_INFINITY;
				n.f = -1;
			}
			tlist.clear();
			toBeChanged.clear();

			for(int i=0;i<tgc.getRpl().length;i++){
				tlist.add(tgc.getRpl()[i]);
			}
			plist2 = ShortestPath.getSPOnGrid(tlist, qRef, toBeChanged, NNdpSelected);
			if (plist2.isEmpty())
				return null;
			NPoint tRef = plist2.get(plist2.size()-1);
			dist += tRef.minDist;
			for(int i : toBeChanged){
				NPoint n = ProcessData.getById(i);
				n.previous = null;
				n.minDist =  Double.POSITIVE_INFINITY;
				n.f = -1;
			}
			toBeChanged.clear();

			plist3 = ShortestPath.getSPOnRawNetwork(NNdpSelected, tRef, toBeChanged);
			if (plist3.isEmpty())
				return null;
			NPoint target = plist3.get(plist3.size()-1);
			dist += target.minDist;
			if(target.equals(NNdpSelected) == false) {
				System.out.println("target not query point\n");
				return null;
			}
			for(int i : toBeChanged){
				NPoint n = ProcessData.getById(i);
				n.previous = null;
				n.minDist =  Double.POSITIVE_INFINITY;
				n.f = -1;
			}

			plist1.addAll(plist2);
			plist1.addAll(plist3);
		}
		return new com.Path(NNdpSelected, q, plist1, dist);
	}

	public static NPoint getById(int id){
		for(Entry<NPoint,List<NPoint>> e: rnet.entrySet()){
			if(e.getKey().getId()==id){
				return e.getKey();
			}
		}
		return null;
	}

	public static void processFile(Path file, String type){
		if(Files.exists(file) && Files.isReadable(file)){
			try {
				BufferedReader reader = Files.newBufferedReader(file, Charset.defaultCharset());
				String line;
				if(type.endsWith("n")){
					double x,y;
					int id;
					String[] el = new String[2];
					int index = 0;
					while((line = reader.readLine()) != null) {
						el = line.split(" ");
						x = Double.parseDouble(el[1]);
						y = Double.parseDouble(el[2]);
						if(type.equals("nn")){
							id = Integer.parseInt(el[0]);
							NPoint n = new NPoint(id,x,y);
							rnet.put(n, null);
							npId.put(id, n);
							if(minx > x)
								minx = x;
							else if(maxx < x)
								maxx = x;
							if(miny > y)
								miny = y;
							else if(maxy < y)
								maxy = y;
						}else{
							if(x>=minx && x<= maxx && y>=miny && y<= maxy){
								DPoint d = new DPoint(index,x,y);
								dpoints.add(d);
								index++;
							}
						}
					}
				}else if(type.equals("ne")){
					String[] el = new String[3];
					int sid,eid;
					while((line = reader.readLine()) != null) {
						el = line.split(" ");
						sid = Integer.parseInt(el[1]);
						eid = Integer.parseInt(el[2]);
						if(!npId.containsKey(sid) ||!npId.containsKey(eid)){
							continue;
						}
						NPoint sn = getById(sid);
						NPoint en = getById(eid);
						if(sn != null){
							List<NPoint> l1 = rnet.get(sn);
							List<NPoint> l2 = rnet.get(en);
							if(l1==null)
								l1 = new ArrayList<NPoint>();
							if(l2==null)
								l2 = new ArrayList<NPoint>();
							l1.add(en);
							l2.add(sn);
							rnet.put(sn, l1);
							rnet.put(en, l2);
						}
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("File invalid");
		}
	}

	@SuppressWarnings("deprecation")
	public static Point2D getIntermediatePoint(NPoint qpoint1, NPoint qpoint2, NPoint qpoint3) {

		Point2D q1 = new Point2D(qpoint1.getX(),qpoint1.getY());
		Point2D q2 = new Point2D(qpoint2.getX(),qpoint2.getY());
		Point2D q3 = new Point2D(qpoint3.getX(),qpoint3.getY());

		boolean flag = false;

		LineSegment2D s12 = new LineSegment2D(q1, q2);
		LineSegment2D s23 = new LineSegment2D(q2, q3);

		ArrayList <Point2D> querypoints = new ArrayList<>();
		querypoints.add(q1);
		querypoints.add(q2);
		querypoints.add(q3);
		queryarea = Polygons2D.convexHull(querypoints);

		double d12 = q1.distance(q2);
		double d23 = q2.distance(q3);
		double d13 = q1.distance(q3);
		ArrayList<Point2D> ip12 = (ArrayList<Point2D>) Circle2D.getIntersections(new Circle2D(s12.firstPoint(), d12), new Circle2D(s12.lastPoint(), d12));
		ArrayList<Point2D> ip23 = (ArrayList<Point2D>) Circle2D.getIntersections(new Circle2D(s23.firstPoint(), d23), new Circle2D(s23.lastPoint(), d23));
		Point2D oq3 = new Point2D();
		Point2D oq1 = new Point2D();
		if(isSameSide(q1,q2,ip12.get(0),q3)){
			oq3 = ip12.get(1);
		}else{
			oq3 = ip12.get(0);
		}
		if(isSameSide(q2,q3,ip23.get(0),q1)){
			oq1 = ip23.get(1);
		}else{
			oq1 = ip23.get(0);
		}
		LineSegment2D line1 = new LineSegment2D(oq3, q3);
		LineSegment2D line2 = new LineSegment2D(oq1, q1);
		Point2D fermat = line1.intersection(line2);

		if(fermat != null){
			if(queryarea.contains(fermat)){
				return fermat;
			}else{
				flag = true;
			}
		}else{
			flag = true;
		}
		if(flag){
			if(d12 > d23 && d12 > d13){
				return q3;
			}else if(d23 > d12 && d23 > d13){
				return q1;
			}else{
				return q2;
			}
		}
		return fermat;
	}

	private static boolean isSameSide(Point2D q1, Point2D q2, Point2D a, Point2D b) {
		double x1 = q1.getX();
		double x2 = q2.getX();
		double y1 = q1.getY();
		double y2 = q2.getY();
		double val = ((y1-y2)*(a.getX()-x1)+(x2-x1)*(a.getY()-y1))*((y1-y2)*(b.getX()-x1)+(x2-x1)*(b.getY()-y1));
		if(val < 0)
			return false;
		return true;
	}
}
