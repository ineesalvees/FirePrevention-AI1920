package Agents;

import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import World.*;
import jade.tools.sniffer.Message;

public class HeadQuarter extends Agent {
	
	private WorldMap map;
	private Position pos;
	
	protected void setup() {
		super.setup();
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getLocalName());
		sd.setType("HeadQuarter");
		dfd.addServices(sd);
		
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		
		addBehaviour(new ReceiveInfo());
		
		map = (WorldMap) getArguments()[0];
		
		pos = new Position(map.getDimension()/2, map.getDimension()/2);
	}
    
	private class ReceiveInfo extends CyclicBehaviour {

		public void action(){
				
			ACLMessage msg = receive();
			
			if (msg==null) {block(); return;}
			
			try{
				Object contentObject = msg.getContentObject();
				switch(msg.getPerformative()) {
					case(ACLMessage.INFORM):
						if(contentObject instanceof Fighter) {
							FighterInfo fInfo = new FighterInfo(((Fighter) contentObject).getName(),((Fighter) contentObject).getPos(),((Fighter) contentObject).isAvailable());
							map.addFighter(fInfo);
							System.out.println("Added agent " + ((Fighter) contentObject).getName());
						}
						if(contentObject instanceof FireStarter) {
							map.setnBurningCells(map.getnBurningCells());
							Fire fire = new Fire(((FireStarter) contentObject).getPos(),((FireStarter) contentObject).getIntensity());
							map.addFire(fire);
							map.changeCellStatus(((FireStarter) contentObject).getPos(),true);
							System.out.println("Cell on position " + ((FireStarter) contentObject).getPos() + " is burning!");
							addBehaviour(new HandlerCheckCombatentes(map.getnBurningCells()));
						}
					/*case(ACLMessage.CONFIRM):
							addBehaviour(new HandlerEscolheCombatente(myAgent,msg));
					case(ACLMessage.ACCEPT_PROPOSAL):
							addBehaviour(new HandlerEnviaCombatente(myAgent,msg)); */
				} 
			}catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private class HandlerCheckCombatentes extends OneShotBehaviour{

		private int fireID;
		
		public HandlerCheckCombatentes(int fireID){
			this.fireID = fireID;
		}

		public void action() {
			Fire targetFire = map.getFires().get(fireID);
			List<FighterInfo> closestFighters = map.calculateClosestFighters(targetFire.getPos());
			FighterInfo chosenFighter = null;

			while(chosenFighter == null) {
				for (FighterInfo fighter : closestFighters) {
					if (fighter.isAvailable()) {
						chosenFighter = fighter;
						break;
					}
				}
			}

			try{
				DFAgentDescription dfd = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("Fighter");
				dfd.addServices(sd);

				DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
				AID provider = new AID();

				if (results.length > 0) {
					for (int i = 0; i < results.length; ++i) {
						DFAgentDescription dfd1 = results[i];
						provider = dfd1.getName();

						if(provider.toString().equals(chosenFighter.getAID())) break;
					}

					System.out.println("Requesting help from fighter: " + chosenFighter.getAID());
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.addReceiver(provider);

					try{
						msg.setContentObject(targetFire);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					System.out.println("Fighter " + chosenFighter.getAID() + " not found!");
				}
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}
		
	}

}
