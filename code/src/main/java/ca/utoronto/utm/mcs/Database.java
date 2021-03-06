package ca.utoronto.utm.mcs;

import static org.neo4j.driver.Values.parameters;

import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Database {
    private Driver driver;
    private String uriDb;

    public Database() {
        uriDb = "bolt://localhost:7687";
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","1234"));
    }

    public boolean checkAndInsertActor(String actorId, String actorName) {
        if(!checkIfActorIdExists(actorId)) {
            insertActor(actorId, actorName);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkIfActorIdExists(String actorId) {
        try (Session session = driver.session())
        {
            try (Transaction tx = session.beginTransaction()) {
                Result node_boolean = tx.run("MATCH (j:Actor {actorId: $x})"
                                + "RETURN j"
                        ,parameters("x", actorId) );
                if (node_boolean.hasNext()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insertActor(String actorId, String actorName) {
        try (Session session = driver.session()){
            session.writeTransaction(tx -> tx.run("CREATE (n:Actor { actorId: $x, actorName: $y })"
                    , parameters("x", actorId, "y", actorName)));
            session.close();
        }
    }

    public boolean checkAndInsertMovie(String movieId, String movieName) {
        if(!checkIfMovieIdExists(movieId)) {
            insertMovie(movieId, movieName);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkIfMovieIdExists(String movieId) {
        try (Session session = driver.session())
        {
            try (Transaction tx = session.beginTransaction()) {
                Result node_boolean = tx.run("MATCH (j:Movie {movieId: $x})"
                                + "RETURN j"
                        ,parameters("x", movieId) );
                if (node_boolean.hasNext()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insertMovie(String movieId, String movieName) {
        try (Session session = driver.session()){
            session.writeTransaction(tx -> tx.run("CREATE (n:Movie { movieId: $x, movieName: $y })"
                    , parameters("x", movieId, "y", movieName)));
            session.close();
        }
    }

    public boolean checkIfRelationShipExists(String actorId, String movieId) {
        try (Session session = driver.session())
        {
            try (Transaction tx = session.beginTransaction()) {
                Result node_boolean = tx.run("RETURN EXISTS( (:Actor {actorId: $x})\n" +
                                "-[:ACTED_IN]-(:Movie {movieId: $y}) ) as bool"
                        ,parameters("x", actorId, "y", movieId) );
                if (node_boolean.hasNext()) {
                    return node_boolean.next().get("bool").toString().equals("TRUE");
                }
            }
        }
        return false;
    }

    public int checkIfHasRelationShip(String actorId, String movieId) {
        if(!(checkIfActorIdExists(actorId) && checkIfMovieIdExists(movieId))) {
            return 404;
        } else {
            return 200;
        }
    }

    public int linkMovieActor(String actorId, String movieId) {
        if(!(checkIfActorIdExists(actorId) && checkIfMovieIdExists(movieId))) {
            return 404;
        } else if (checkIfRelationShipExists(actorId, movieId)) {
            return 400;
        } else {
            try (Session session = driver.session()){
                session.writeTransaction(tx -> tx.run("MATCH (a:Actor {actorId:$x}),"
                        + "(t:Movie {movieId:$y})\n" +
                        "MERGE (a)-[r:ACTED_IN]->(t)\n" +
                        "RETURN r", parameters("x", actorId, "y", movieId)));
                session.close();
                return 200;
            }
        }
    }

    public String getActorName(String actorId) {
        if(!(checkIfActorIdExists(actorId))) {
            return "";
        } else {
            try (Session session = driver.session()){
                try (Transaction tx = session.beginTransaction()) {
                    Result actorName = tx.run("MATCH (a:Actor {actorId:$x})"
                            + "RETURN a.actorName as name", parameters("x", actorId));

                    String name = actorName.next().get("name").toString().replace("\"", "");
                    System.out.println(name);
                    return name;
                }
            }
        }
    }


    public List<String> getMoviesActedIn(String actorId) {
        if(!(checkIfActorIdExists(actorId))) {
            return null;
        } else {
            try (Session session = driver.session()){
                try (Transaction tx = session.beginTransaction()) {
                    Result moviesActedIn = tx.run("MATCH (a:Actor {actorId:$x})-->(Movie)"
                            + "RETURN Movie.movieId", parameters("x", actorId));
                    List<Record> movies = new ArrayList<Record>();
                    if(moviesActedIn.hasNext()) {
                        movies = moviesActedIn.list();
                    }
                    String movies_ids = "";
                    for(int i = 0; i<movies.size(); i++) {
                        String id = movies.get(i).values().toString();
                        movies_ids = movies_ids + id.substring(2,id.length()-2) + ",";
                    }
                    movies_ids = movies_ids.substring(0, movies_ids.length()-1);
                    List<String> myList = new ArrayList<String>(Arrays.asList(movies_ids.split(",")));
                    return myList;
                }
            }
        }
    }

    public String getMovieName(String movieId) {
        if(!(checkIfMovieIdExists(movieId))) {
            return "";
        } else {
            try (Session session = driver.session()){
                try (Transaction tx = session.beginTransaction()) {
                    Result movieName = tx.run("MATCH (a:Movie {movieId:$x})"
                            + "RETURN a.movieName as name", parameters("x", movieId));
                    String name = movieName.next().get("name").toString().replace("[\"", "").replace("\"]", "").replace("\"", "");
                    System.out.println(name);
                    return name;

                }
            }
        }
    }

    public List<String> getActorsActedIn(String movieId) {
        if(!(checkIfMovieIdExists(movieId))) {
            return null;
        } else {
            try (Session session = driver.session()){
                try (Transaction tx = session.beginTransaction()) {
                    Result actorsActedIn = tx.run("MATCH (a:Movie {movieId:$x})<--(Actor)"
                            + "RETURN Actor.actorId", parameters("x", movieId));
                    List<Record> actors = new ArrayList<Record>();
                    if(actorsActedIn.hasNext()) {
                        actors = actorsActedIn.list();
                    }
                    String actors_ids = "";
                    for(int i = 0; i<actors.size(); i++) {
                        String id = actors.get(i).values().toString();
                        actors_ids = actors_ids + id.substring(2,id.length()-2) + ",";
                    }
                    actors_ids = actors_ids.substring(0, actors_ids.length()-1);
                    List<String> myList = new ArrayList<String>(Arrays.asList(actors_ids.split(",")));

                    return myList;

                }
            }
        }
    }

    public String computeBaconNumber(String actorId) {
        try (Session session = driver.session())
        {
            if(getActorName(actorId).equals("[\"Kevin Bacon\"]")) {
                return "0";
            } else if(getActorName(actorId).equals("")) {
                return "400";
            } else {
                try (Transaction tx = session.beginTransaction()) {
                    Result node_boolean = tx.run("MATCH (k:Actor { actorName: 'Kevin Bacon' }),(m:Actor { actorId: $x }), p = shortestPath((k)-[:ACTED_IN*]-(m))\n" +
                                    "RETURN length(p)-1 as length"
                            , parameters("x", actorId));
                    if (node_boolean.hasNext()) {
                        try {
                            String code = node_boolean.next().get("length").toString();
                            return code;
                        } catch (Exception e) {
                            return "0";
                        }
                    } else {
                        // no paths exist
                        return "404";
                    }
                }
            }
        }
    }

    public void close() {
        driver.close();
    }

}
