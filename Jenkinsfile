pipeline {
    agent any
    tools {
        maven 'Maven 3.6.3'
        jdk 'Java 8'
    }
     stages {
        stage ('Build') {
            steps {
                sh 'mvn -U clean install deploy -P civ-jenkins'
            }
        }
        stage ('Trigger cascading builds') {
            when {
                expression {
                    env.BRANCH_NAME == 'master'
                }
            }
            steps {
                build job: '../NameLayer/master', wait: false
            }
        }
    }
    post {
        always {
            withCredentials([string(credentialsId: 'civclassic-discord-webhook', variable: 'DISCORD_WEBHOOK')]) {
                discordSend description: "**Build:** [${currentBuild.id}](${env.BUILD_URL})\n**Status:** [${currentBuild.currentResult}](${env.BUILD_URL})\n", footer: 'Civclassic Jenkins', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: "${env.JOB_NAME} #${currentBuild.id}", webhookURL: DISCORD_WEBHOOK
            }
        }
    }
}
