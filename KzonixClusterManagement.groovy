@Grapes(
        @Grab(group = 'io.github.cdimascio', module = 'dotenv-java', version = '2.2.0')
)
import io.github.cdimascio.dotenv.Dotenv

import java.util.function.Function
import java.util.stream.Collectors

dotenv = Dotenv.configure().load();

def readEnvVariable(String name) {
    return System.getenv(name) ?: dotenv.get(name)
}

def readEnvVariable(String name, String promptAskPhrase) {
    return readEnvVariable(name) ?: System.console().readLine(" > $promptAskPhrase\n")
}

def workerUsername = readEnvVariable("WORKER_USERNAME", "What is username for worker nodes?")
def workerPassword = readEnvVariable("WORKER_PASSWORD", "What is password for $workerUsername")
def masterNodePassword = readEnvVariable("MASTER_PASSWORD", "What is username for master node?")
def masterNodeUsername = readEnvVariable("MASTER_USERNAME", "What is password for $masterNodePassword")


class ServerNode {
    String id
    String host
    Integer port
    String usr
    String pass

}

Map<String, ServerNode> serverNodes = [
        new ServerNode(id: 'rock64 - 1', host: '192.168.68.171', port: 22, usr: workerUsername, pass: workerPassword),
        new ServerNode(id: 'rock64 - 2', host: '192.168.68.172', port: 22, usr: workerUsername, pass: workerPassword),
        new ServerNode(id: 'rock64 - 3', host: '192.168.68.173', port: 22, usr: workerUsername, pass: workerPassword),
        new ServerNode(id: 'rpi3-node1', host: '192.168.68.161', port: 22, usr: workerUsername, pass: workerPassword),
        new ServerNode(id: 'rpi3-node2', host: '192.168.68.162', port: 22, usr: workerUsername, pass: workerPassword),
        new ServerNode(id: 'rpi4-node1', host: '192.168.68.151', port: 22, usr: workerUsername, pass: workerPassword),
        new ServerNode(id: 'rpi4-node2', host: '192.168.68.152', port: 22, usr: workerUsername, pass: workerPassword)
].stream().collect(Collectors.toMap({ it.id }, Function.identity()))


action name: 'updateServerNodes', closure: {
    info "Running action on $node.id" // a node reference is available in the current context
    String nodeId = node.id
    pass = serverNodes[nodeId].pass
    shell sudopass: pass, command: "apt update"
    res = shell sudopass: pass, command: "apt upgrade -y"
    res = res.output.split('\n')
    info "$node.id: ${res[-1]}"
    res = shell command: "uptime"
    info "$node.id: $res.output"
}


inlineInventory {
    serverNodes.each {
        node id: it.key, host: it.value.host, port: it.value.port, username: it.value.usr, password: it.value.pass
    }

}.provision {
    task name: 'Update Server Nodes', parallel: serverNodes.size(), actions: {
        updateServerNodes()
    }
}
