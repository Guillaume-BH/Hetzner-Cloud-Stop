package fr.nexoid.hetzner;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class HetznerStop extends JavaPlugin {

    private final static String LOG_PREFIX = "[Reloaded-MC - Hetzner-Cloud-Stop] ";
    private String apiToken;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.apiToken = this.getConfig().getString("HETZNER_API_TOKEN");
    }

    @Override
    public void onDisable() {
        try {
            Map<String, String> serverList = getInstancesList();
            for (Map.Entry<String, String> entry : serverList.entrySet()) {
                if (entry.getValue().equals(this.getPublicAddress())) {
                    this.stopServer(entry.getKey());
                    System.out.println(LOG_PREFIX + "Successfully deleted this server from OVH.");
                    break;
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the server by send a DELETE request to the Hetzner Cloud API
     *
     * @param idServer the id of the server to stop
     * @throws IOException if an error occurs while stopping the server
     */
    private void stopServer(String idServer) throws IOException, NoSuchAlgorithmException {
        Response response = Request.delete("https://api.hetzner.cloud/v1/servers/" + idServer)
                .addHeader("Authorization", "Bearer " + apiToken)
                .execute();
        int statusCode = response.returnResponse().getCode();
        if (statusCode == 200) {
            System.out.println(LOG_PREFIX + "Server with ID " + idServer + " has been successfully deleted.");
        } else {
            System.out.println(LOG_PREFIX + "Failed to delete server with ID " + idServer + ". Status code: " + statusCode);
        }
    }

    /**
     * Get instances list
     *
     * @return a map of instances id and their public IP
     * @throws IOException if an error occurs while getting the instances list
     */
    private Map<String, String> getInstancesList() throws IOException, NoSuchAlgorithmException {
        Response response = Request.get("https://api.hetzner.cloud/v1/servers")
                .addHeader("Authorization", "Bearer " + apiToken)
                .execute();

        JSONArray servers = new JSONObject(response.returnContent().asString()).getJSONArray("servers");
        Map<String, String> instances = new HashMap<>();
        for (int i = 0; i < servers.length(); i++) {
            JSONObject s = servers.getJSONObject(i);
            String id = s.getString("id");
            String ip = s.getJSONObject("public_net").getJSONObject("ipv4").getString("ip");
            instances.put(id, ip);
        }
        return instances;
    }

    /**
     * Get the public IP of the minecraft server
     *
     * @return the public IP of the minecraft server
     * @throws IOException if an error occurs while getting the public IP
     */
    private String getPublicAddress() throws IOException {
        try {
            Process process = Runtime.getRuntime().exec("curl ifconfig.me");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String publicIp = reader.readLine();
            process.waitFor();
            return publicIp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
