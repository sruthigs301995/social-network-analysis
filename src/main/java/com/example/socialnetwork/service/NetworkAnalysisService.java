package com.example.socialnetwork.service;

import com.example.socialnetwork.model.User;
import com.example.socialnetwork.repository.UserRepository;
import org.jgrapht.Graph;
import org.jgrapht.alg.clustering.LabelPropagationClustering;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NetworkAnalysisService {
    @Autowired
    private UserRepository userRepository;

    public List<User> findShortestPath(Long userId1, Long userId2) {
        User start = userRepository.findById(userId1).orElseThrow();
        User end = userRepository.findById(userId2).orElseThrow();

        Queue<User> queue = new LinkedList<>();
        Map<User, User> previous = new HashMap<>();
        Set<User> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            User current = queue.poll();
            if (current.equals(end)) {
                return reconstructPath(previous, start, end);
            }

            for (User friend : current.getFriends()) {
                if (!visited.contains(friend)) {
                    queue.add(friend);
                    visited.add(friend);
                    previous.put(friend, current);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    private List<User> reconstructPath(Map<User, User> previous, User start, User end) {
        List<User> path = new ArrayList<>();
        for (User at = end; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    public List<Map<String, Object>> identifyCommunities() {
        List<User> users = userRepository.findAll();
        Graph<User, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        // Add vertices
        users.forEach(graph::addVertex);

        // Add edges
        for (User user : users) {
            for (User friend : user.getFriends()) {
                graph.addEdge(user, friend);
            }
        }

        // Apply Label Propagation Clustering
        LabelPropagationClustering<User, DefaultEdge> clustering = new LabelPropagationClustering<>(graph);
        List<Set<User>> communities = clustering.getClustering().getClusters();

        // Convert community structure to list of maps with communityId and users
        List<Map<String, Object>> communityList = new ArrayList<>();
        int communityId = 1;
        for (Set<User> community : communities) {
            Map<String, Object> communityMap = new HashMap<>();
            communityMap.put("communityId", communityId++);
            communityMap.put("users", community);
            communityList.add(communityMap);
        }

        return communityList;
    }

    public Map<Long, Integer> calculateDegreeCentrality() {
        Map<Long, Integer> degreeCentrality = new HashMap<>();
        List<User> users = userRepository.findAll();
        for (User user : users) {
            degreeCentrality.put(user.getId(), user.getFriends().size());
        }
        return degreeCentrality;
    }
}
